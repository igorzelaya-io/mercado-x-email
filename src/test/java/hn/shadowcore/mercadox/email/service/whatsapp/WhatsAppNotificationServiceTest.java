package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.email.config.WhatsAppWebClientFactory;
import hn.shadowcore.mercadox.email.service.NotificationTemplateService;
import hn.shadowcore.mercadox.library.entity.avro.LeadCreatedEvent;
import hn.shadowcore.mercadox.library.entity.model.ai.OrganizationWhatsAppConfig;
import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.model.enums.TemplateChannel;
import hn.shadowcore.mercadox.library.entity.response.dto.NotificationRequest;
import hn.shadowcore.mercadox.library.jpa.repository.OrganizationWhatsAppConfigRepository;
import hn.shadowcore.mercadox.email.exception.WhatsAppClientException;
import hn.shadowcore.mercadox.email.exception.WhatsAppServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppNotificationServiceTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    // Extracted so .onStatus() can be stubbed to return itself — the chain is
    // now retrieve() → onStatus(4xx) → onStatus(5xx) → bodyToMono() → block(),
    // and this mock absorbs any future onStatus() additions without breaking tests.
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private WhatsAppWebClientFactory webClientFactory;

    @Mock
    private OrganizationWhatsAppConfigRepository configRepository;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private AbstractWhatsAppNotificationHandler<LeadCreatedEvent> leadWelcomeHandler;

    private WhatsAppNotificationService service;
    private LeadCreatedEvent event;
    private NotificationRequest request;

    @BeforeEach
    void setUp() {
        event = LeadCreatedEvent.newBuilder()
                .setOrgName("MercadoX")
                .setUserName("Igor")
                .setEmail("igor@example.com")
                .setPhoneNumber("+50499998888")
                .setOrgId(ORG_ID.toString())
                .setEventId(java.util.UUID.randomUUID().toString())
                .setEventType("LEAD_CREATED")
                .setOccurredAt(java.time.Instant.now().toString())
                .build();

        request = NotificationRequest.builder()
                .orgId(ORG_ID.toString())
                .templateKey("LEAD_CREATION_TEMPLATE")
                .phoneNumber("+50499998888")
                .variables(Map.of("userName", "Igor", "orgName", "MercadoX"))
                .build();

        NotificationTemplate template = NotificationTemplate.builder()
                .whatsappTemplateName("lead_welcome")
                .languageCode("es_HN")
                .variables(List.of("userName", "orgName"))
                .build();

        OrganizationWhatsAppConfig tenantConfig = OrganizationWhatsAppConfig.create(
                ORG_ID, "mock-phone-number-id", "mock-waba-id", "mock-access-token", null);

        lenient().when(webClient.post().uri(anyString()).bodyValue(any()).retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        lenient().when(leadWelcomeHandler.eventType()).thenReturn(LeadCreatedEvent.class);
        lenient().when(leadWelcomeHandler.buildRequest(event)).thenReturn(request);
        lenient().when(notificationTemplateService.findByNameAndChannel("LEAD_CREATION_TEMPLATE", TemplateChannel.WHATSAPP))
                .thenReturn(template);
        lenient().when(configRepository.findByOrganizationId(ORG_ID)).thenReturn(Optional.of(tenantConfig));
        lenient().when(webClientFactory.forTenant(tenantConfig)).thenReturn(webClient);

        service = new WhatsAppNotificationService(
                webClientFactory, configRepository, notificationTemplateService, List.of(leadWelcomeHandler));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsWhatsAppTemplateMessageWhenApiConfirmsDelivery() {
        WhatsAppMessageResponse response =
                new WhatsAppMessageResponse(List.of(new WhatsAppMessageResponse.Message("wamid.abc123")));

        when(responseSpec.bodyToMono(WhatsAppMessageResponse.class).block(any(Duration.class)))
                .thenReturn(response);

        service.handle(event);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webClient.post().uri(anyString()), atLeastOnce()).bodyValue(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsEntry("to", "+50499998888");
    }

    @Test
    void throwsWhenNoHandlerIsRegisteredForEventType() {
        WhatsAppNotificationService emptyHandlerService = new WhatsAppNotificationService(
                webClientFactory, configRepository, notificationTemplateService, List.of());

        assertThatThrownBy(() -> emptyHandlerService.handle(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LeadCreatedEvent");
    }

    @Test
    void throwsWhenNoTenantConfigFoundForOrganization() {
        lenient().when(configRepository.findByOrganizationId(ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(ORG_ID.toString());
    }

    @Test
    void throwsWhenApiReturnsNoMessages() {
        when(responseSpec.bodyToMono(WhatsAppMessageResponse.class).block(any(Duration.class)))
                .thenReturn(new WhatsAppMessageResponse(List.of()));

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("+50499998888");
    }

    @Test
    void throwsWhenApiCallReturnsNullResponse() {
        when(responseSpec.bodyToMono(WhatsAppMessageResponse.class).block(any(Duration.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("+50499998888");
    }

    @Test
    void throwsWhatsAppClientExceptionOn4xxResponse() {
        WhatsAppClientException ex = new WhatsAppClientException(400, "Invalid phone number");
        when(responseSpec.bodyToMono(WhatsAppMessageResponse.class).block(any(Duration.class)))
                .thenThrow(ex);

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(WhatsAppClientException.class)
                .hasMessageContaining("400");
    }

    @Test
    void throwsWhatsAppServerExceptionOn5xxResponse() {
        WhatsAppServerException ex = new WhatsAppServerException(503, "Service Unavailable");
        when(responseSpec.bodyToMono(WhatsAppMessageResponse.class).block(any(Duration.class)))
                .thenThrow(ex);

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(WhatsAppServerException.class)
                .hasMessageContaining("503");
    }

    @Test
    void propagatesWhatsAppApiFailureUncaught() {
        RuntimeException apiFailure = new RuntimeException("WhatsApp API unavailable");
        when(responseSpec.bodyToMono(WhatsAppMessageResponse.class).block(any(Duration.class)))
                .thenThrow(apiFailure);

        assertThatThrownBy(() -> service.handle(event)).isSameAs(apiFailure);
    }
}
