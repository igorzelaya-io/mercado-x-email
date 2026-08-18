package hn.shadowcore.mercadox.email.listener;

import hn.shadowcore.mercadox.email.exception.WhatsAppClientException;
import hn.shadowcore.mercadox.email.service.mailer.EmailOrchestratorService;
import hn.shadowcore.mercadox.email.service.whatsapp.WhatsAppNotificationService;
import hn.shadowcore.mercadox.library.entity.avro.LeadCreatedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "lead.created.v1",
                "lead.created.v1.DLT"
        },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@Testcontainers
class DltIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideInfrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @MockitoBean
    private WhatsAppNotificationService whatsAppNotificationService;

    @MockitoBean
    private EmailOrchestratorService emailOrchestratorService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushAll();
        reset(whatsAppNotificationService);
    }

    @Test
    void nullEventId_routesToDlt_withoutCallingService() {
        LeadCreatedEvent event = LeadCreatedEvent.newBuilder()
                .setOrgName("MercadoX")
                .setUserName("Igor")
                .setEmail("igor@test.com")
                .setPhoneNumber("+50499998888")
                .setOrgId("org-001")
                .setEventId(null)
                .setEventType("LEAD_CREATED")
                .setOccurredAt(java.time.Instant.now().toString())
                .build();

        kafkaTemplate.send("lead.created.v1", "any-key", event);

        // The aspect throws InvalidEventIdException → non-retryable → message goes straight to DLT.
        // The service is never reached.
        Awaitility.await()
                .pollDelay(3, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        org.mockito.Mockito.verifyNoInteractions(whatsAppNotificationService));

        // No Redis key written — event was rejected before the idempotency claim
        org.assertj.core.api.Assertions
                .assertThat(redisTemplate.keys("eventId:*")).isEmpty();
    }

    @Test
    void nonRetryableClientException_routesToDltWithoutRetry() {
        LeadCreatedEvent event = buildEvent(UUID.randomUUID().toString());

        doThrow(new WhatsAppClientException(400, "Invalid phone number"))
                .when(whatsAppNotificationService).handle(any());

        kafkaTemplate.send("lead.created.v1", event.getEventId(), event);

        // WhatsAppClientException is non-retryable → goes to DLT on first failure.
        // Service is called exactly once — no retries.
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(whatsAppNotificationService, org.mockito.Mockito.times(1)).handle(any()));

        // Give error handler time to route to DLT; verify service not called again
        Awaitility.await()
                .pollDelay(3, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(whatsAppNotificationService, org.mockito.Mockito.times(1)).handle(any()));
    }

    @Test
    void retryableServerException_serviceCalledOnce_retryBlockedByIdempotency() {
        LeadCreatedEvent event = buildEvent(UUID.randomUUID().toString());

        doThrow(new RuntimeException("503 Service Unavailable"))
                .when(whatsAppNotificationService).handle(any());

        kafkaTemplate.send("lead.created.v1", event.getEventId(), event);

        // Service is reached exactly once: claimProcessing() claims the eventId in Redis
        // BEFORE proceed() runs. When the service throws, the error handler schedules a retry,
        // but that retry finds the key already in Redis → aspect returns null (duplicate dropped)
        // → Kafka sees a clean return → stops retrying. At-most-once delivery is intentional
        // for notifications where duplicates are worse than a missed retry.
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(whatsAppNotificationService, org.mockito.Mockito.times(1)).handle(any()));

        Awaitility.await()
                .pollDelay(3, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(whatsAppNotificationService, org.mockito.Mockito.times(1)).handle(any()));

        assertThat(redisTemplate.hasKey("eventId:" + event.getEventId())).isTrue();
    }

    private LeadCreatedEvent buildEvent(String eventId) {
        return LeadCreatedEvent.newBuilder()
                .setOrgName("MercadoX")
                .setUserName("Igor")
                .setEmail("igor@test.com")
                .setPhoneNumber("+50499998888")
                .setOrgId("org-001")
                .setEventId(eventId)
                .setEventType("LEAD_CREATED")
                .setOccurredAt(java.time.Instant.now().toString())
                .build();
    }
}
