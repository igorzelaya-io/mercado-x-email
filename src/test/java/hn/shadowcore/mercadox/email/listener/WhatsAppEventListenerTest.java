package hn.shadowcore.mercadox.email.listener;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"lead.created.v1"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@Testcontainers
class WhatsAppEventListenerTest {

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
    private StringRedisTemplate redisTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushAll();
        reset(whatsAppNotificationService);
    }

    @Test
    void happyPath_newEvent_delegatesToServiceAndMarksEventIdInRedis() {
        LeadCreatedEvent event = buildEvent(UUID.randomUUID().toString());

        kafkaTemplate.send("lead.created.v1", event.getEventId(), event);

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(whatsAppNotificationService).handle(any(LeadCreatedEvent.class)));

        assertThat(redisTemplate.hasKey("eventId:" + event.getEventId())).isTrue();
    }

    @Test
    void duplicateEvent_sameEventId_serviceCalledOnlyOnce() {
        LeadCreatedEvent event = buildEvent(UUID.randomUUID().toString());

        kafkaTemplate.send("lead.created.v1", event.getEventId(), event);

        // Wait until the first delivery commits the eventId to Redis
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(redisTemplate.hasKey("eventId:" + event.getEventId())).isTrue());

        kafkaTemplate.send("lead.created.v1", event.getEventId(), event);

        // Give the consumer time to receive and skip the duplicate
        Awaitility.await()
                .pollDelay(3, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(whatsAppNotificationService, times(1)).handle(any(LeadCreatedEvent.class)));
    }

    @Test
    void nullEventId_aspectShortCircuits_serviceAndRedisNeverTouched() {
        LeadCreatedEvent event = buildEvent(null);

        kafkaTemplate.send("lead.created.v1", "any-key", event);

        Awaitility.await()
                .pollDelay(3, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> verifyNoInteractions(whatsAppNotificationService));

        assertThat(redisTemplate.keys("eventId:*")).isEmpty();
    }

    @Test
    void serviceThrowsException_eventIdNotMarkedProcessed_aspectAllowsRetry() {
        LeadCreatedEvent event = buildEvent(UUID.randomUUID().toString());
        doThrow(new RuntimeException("WhatsApp API unavailable"))
                .when(whatsAppNotificationService).handle(any());

        kafkaTemplate.send("lead.created.v1", event.getEventId(), event);

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(whatsAppNotificationService, atLeastOnce()).handle(any(LeadCreatedEvent.class)));

        // handle() threw so the aspect skipped markProcessed() — key must be absent
        assertThat(redisTemplate.hasKey("eventId:" + event.getEventId())).isFalse();
    }

    private LeadCreatedEvent buildEvent(String eventId) {
        return LeadCreatedEvent.newBuilder()
                .setOrgName("MercadoX")
                .setUserName("Igor")
                .setEmail("igor@example.com")
                .setPhoneNumber("+50499998888")
                .setOrgId("org-001")
                .setEventId(eventId)
                .setEventType("LEAD_CREATED")
                .setOccurredAt(java.time.Instant.now().toString())
                .build();
    }
}
