package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.email.service.NotificationTemplateService;
import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.model.enums.TemplateChannel;
import hn.shadowcore.mercadox.library.entity.model.enums.kafka.event.LeadCreatedEvent;
import hn.shadowcore.mercadox.library.entity.response.dto.NotificationRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppNotificationServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private AbstractWhatsAppNotificationHandler<LeadCreatedEvent> leadWelcomeHandler;

    private WhatsAppNotificationService service;
    private LeadCreatedEvent event;
    private NotificationRequest request;

    @BeforeEach
    void setUp() {
        event = new LeadCreatedEvent("MercadoX", "Igor", "igor@example.com", "+50499998888");
        event.setOrgId("org-123");

        request = NotificationRequest.builder()
                .orgId("org-123")
                .templateKey("LEAD_CREATION_TEMPLATE")
                .phoneNumber("+50499998888")
                .variables(Map.of("userName", "Igor", "orgName", "MercadoX"))
                .build();

        NotificationTemplate template = NotificationTemplate.builder()
                .whatsappTemplateName("lead_welcome")
                .languageCode("es_HN")
                .variables(List.of("userName", "orgName"))
                .build();

        // lenient: the handler-not-found test builds its own service with no handlers,
        // so these stubs are never exercised there.
        lenient().when(leadWelcomeHandler.eventType()).thenReturn(LeadCreatedEvent.class);
        lenient().when(leadWelcomeHandler.buildRequest(event)).thenReturn(request);
        lenient().when(notificationTemplateService.findByNameAndChannel("LEAD_CREATION_TEMPLATE", TemplateChannel.WHATSAPP))
                .thenReturn(template);

        service = new WhatsAppNotificationService(webClient, notificationTemplateService, List.of(leadWelcomeHandler));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsWhatsAppTemplateMessageWhenApiConfirmsDelivery() {
        WhatsAppMessageResponse response =
                new WhatsAppMessageResponse(List.of(new WhatsAppMessageResponse.Message("wamid.abc123")));

        when(webClient.post().uri("/messages").bodyValue(any())
                .retrieve().bodyToMono(WhatsAppMessageResponse.class).block(any(Duration.class)))
                .thenReturn(response);

        service.handle(event);

        // atLeastOnce: with RETURNS_DEEP_STUBS, re-evaluating the chain expression here to get a
        // verifiable handle on the nested mock re-triggers bodyValue() once more (the stub-setup
        // call above already counted as one invocation), so a strict times(1) over-counts.
        // getValue() still returns the last (real) invocation's argument.
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webClient.post().uri("/messages"), atLeastOnce()).bodyValue(payloadCaptor.capture());

        Map<String, Object> capturedPayload = payloadCaptor.getValue();
        assertThat(capturedPayload).containsEntry("to", "+50499998888");
    }

    @Test
    void throwsWhenNoHandlerIsRegisteredForEventType() {
        WhatsAppNotificationService emptyHandlerService =
                new WhatsAppNotificationService(webClient, notificationTemplateService, List.of());

        assertThatThrownBy(() -> emptyHandlerService.handle(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LeadCreatedEvent");
    }

    @Test
    void throwsWhenApiReturnsNoMessages() {
        WhatsAppMessageResponse emptyResponse = new WhatsAppMessageResponse(List.of());

        when(webClient.post().uri("/messages").bodyValue(any())
                .retrieve().bodyToMono(WhatsAppMessageResponse.class).block(any(Duration.class)))
                .thenReturn(emptyResponse);

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("+50499998888");
    }

    @Test
    void throwsWhenApiCallReturnsNullResponse() {
        when(webClient.post().uri("/messages").bodyValue(any())
                .retrieve().bodyToMono(WhatsAppMessageResponse.class).block(any(Duration.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("+50499998888");
    }

    @Test
    void propagatesWhatsAppApiFailureUncaught() {
        // Locks in current behavior: the service does not catch/swallow WebClient failures.
        // This is what allows Kafka's container-level retry/DLT handling to see the failure at all.
        RuntimeException apiFailure = new RuntimeException("WhatsApp API unavailable");

        when(webClient.post().uri("/messages").bodyValue(any())
                .retrieve().bodyToMono(WhatsAppMessageResponse.class).block(any(Duration.class)))
                .thenThrow(apiFailure);

        assertThatThrownBy(() -> service.handle(event)).isSameAs(apiFailure);
    }
}
