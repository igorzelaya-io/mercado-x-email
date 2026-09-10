package hn.shadowcore.mercadox.email.listener;

import hn.shadowcore.mercadox.email.exception.WhatsAppRateLimitException;
import hn.shadowcore.mercadox.email.exception.WhatsAppClientException;
import hn.shadowcore.mercadox.email.exception.WhatsAppServerException;
import hn.shadowcore.mercadox.email.service.NotificationTemplateService;
import hn.shadowcore.mercadox.email.service.whatsapp.WhatsAppFreeformService;
import hn.shadowcore.mercadox.library.entity.avro.AiReplyGeneratedEvent;
import hn.shadowcore.mercadox.library.entity.model.ai.OrganizationWhatsAppConfig;
import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.model.enums.TemplateChannel;
import hn.shadowcore.mercadox.library.jpa.repository.OrganizationWhatsAppConfigRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiReplyConsumerListenerTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String RECIPIENT = "+50499998888";

    @Mock
    private WhatsAppFreeformService freeformService;

    @Mock
    private OrganizationWhatsAppConfigRepository configRepository;

    @Mock
    private NotificationTemplateService templateService;

    @Test
    void missingRecipient_dropsEventAndAlwaysClearsMdc() {
        AiReplyConsumerListener listener = listener(webClient(HttpStatus.OK, successBody(), null));

        listener.handleAiReply(record(event(null, "hello", null, null)));

        verifyNoInteractions(freeformService, configRepository, templateService);
        assertThat(MDC.get("eventId")).isNull();
    }

    @Test
    void freeformReply_normalizesNullTextAndDoesNotLoadTemplate() {
        AiReplyConsumerListener listener = listener(webClient(HttpStatus.OK, successBody(), null));

        listener.handleAiReply(record(event(RECIPIENT, null, "FREEFORM", null)));

        verify(freeformService).sendText(RECIPIENT, "");
        verifyNoInteractions(configRepository, templateService);
        assertThat(MDC.get("eventId")).isNull();
    }

    @Test
    void templateReply_withoutConfiguredTemplateIsDropped() {
        when(configRepository.findByOrganizationId(ORG_ID)).thenReturn(Optional.empty());
        AiReplyConsumerListener listener = listener(webClient(HttpStatus.OK, successBody(), null));

        listener.handleAiReply(record(event(RECIPIENT, "hello", "TEMPLATE", ORG_ID.toString())));

        verify(templateService, never()).findByNameAndChannel(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        assertThat(MDC.get("eventId")).isNull();
    }

    @Test
    void templateReply_withoutDefaultTemplateIsDropped() {
        OrganizationWhatsAppConfig config = OrganizationWhatsAppConfig.create(
                ORG_ID, "phone-id", "waba-id", "token", null);
        when(configRepository.findByOrganizationId(ORG_ID)).thenReturn(Optional.of(config));
        AiReplyConsumerListener listener = listener(webClient(HttpStatus.OK, successBody(), null));

        listener.handleAiReply(record(event(RECIPIENT, "hello", "TEMPLATE", ORG_ID.toString())));

        verifyNoInteractions(templateService);
        assertThat(MDC.get("eventId")).isNull();
    }

    @Test
    void templateReply_sendsConfiguredReengagementTemplate() {
        OrganizationWhatsAppConfig config = OrganizationWhatsAppConfig.create(
                ORG_ID, "phone-id", "waba-id", "token", "return_to_chat");
        NotificationTemplate template = NotificationTemplate.builder()
                .whatsappTemplateName("return_to_chat")
                .languageCode("es_HN")
                .variables(List.of())
                .build();
        when(configRepository.findByOrganizationId(ORG_ID)).thenReturn(Optional.of(config));
        when(templateService.findByNameAndChannel("return_to_chat", TemplateChannel.WHATSAPP))
                .thenReturn(template);
        AiReplyConsumerListener listener = listener(webClient(HttpStatus.OK, successBody(), null));

        listener.handleAiReply(record(event(RECIPIENT, "hello", "TEMPLATE", ORG_ID.toString())));

        verify(templateService).findByNameAndChannel("return_to_chat", TemplateChannel.WHATSAPP);
        assertThat(MDC.get("eventId")).isNull();
    }

    @Test
    void templateReply_mapsMetaRateLimitAndRetryAfterHeader() {
        OrganizationWhatsAppConfig config = OrganizationWhatsAppConfig.create(
                ORG_ID, "phone-id", "waba-id", "token", "return_to_chat");
        NotificationTemplate template = NotificationTemplate.builder()
                .whatsappTemplateName("return_to_chat")
                .languageCode("es_HN")
                .variables(List.of())
                .build();
        when(configRepository.findByOrganizationId(ORG_ID)).thenReturn(Optional.of(config));
        when(templateService.findByNameAndChannel("return_to_chat", TemplateChannel.WHATSAPP))
                .thenReturn(template);
        AiReplyConsumerListener listener = listener(
                webClient(HttpStatus.TOO_MANY_REQUESTS, "throttled", "30"));

        assertThatThrownBy(() -> listener.handleAiReply(
                record(event(RECIPIENT, "hello", "TEMPLATE", ORG_ID.toString()))))
                .isInstanceOfSatisfying(WhatsAppRateLimitException.class,
                        exception -> assertThat(exception.getRetryAfterSeconds()).isEqualTo(30L));
        assertThat(MDC.get("eventId")).isNull();
    }

    @Test
    void templateReply_mapsPermanentClientFailure() {
        AiReplyConsumerListener listener = configuredTemplateListener(
                webClient(HttpStatus.BAD_REQUEST, "bad template", null));

        assertThatThrownBy(() -> listener.handleAiReply(
                record(event(RECIPIENT, "hello", "TEMPLATE", ORG_ID.toString()))))
                .isInstanceOf(WhatsAppClientException.class)
                .hasMessageContaining("400");
    }

    @Test
    void templateReply_mapsServerFailure() {
        AiReplyConsumerListener listener = configuredTemplateListener(
                webClient(HttpStatus.SERVICE_UNAVAILABLE, "try later", null));

        assertThatThrownBy(() -> listener.handleAiReply(
                record(event(RECIPIENT, "hello", "TEMPLATE", ORG_ID.toString()))))
                .isInstanceOf(WhatsAppServerException.class)
                .hasMessageContaining("503");
    }

    private AiReplyConsumerListener listener(WebClient webClient) {
        return new AiReplyConsumerListener(freeformService, configRepository, templateService, webClient);
    }

    private AiReplyConsumerListener configuredTemplateListener(WebClient webClient) {
        OrganizationWhatsAppConfig config = OrganizationWhatsAppConfig.create(
                ORG_ID, "phone-id", "waba-id", "token", "return_to_chat");
        NotificationTemplate template = NotificationTemplate.builder()
                .whatsappTemplateName("return_to_chat")
                .languageCode("es_HN")
                .variables(List.of())
                .build();
        when(configRepository.findByOrganizationId(ORG_ID)).thenReturn(Optional.of(config));
        when(templateService.findByNameAndChannel("return_to_chat", TemplateChannel.WHATSAPP))
                .thenReturn(template);
        return listener(webClient);
    }

    private static ConsumerRecord<String, AiReplyGeneratedEvent> record(AiReplyGeneratedEvent event) {
        return new ConsumerRecord<>("ai.reply.generated.v1", 0, 0L, event.getEventId(), event);
    }

    private static AiReplyGeneratedEvent event(String recipient, String text, String sendMode, String orgId) {
        return AiReplyGeneratedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType("AI_REPLY_GENERATED")
                .setOrgId(orgId)
                .setOccurredAt(Instant.now().toString())
                .setChannel("WHATSAPP")
                .setRecipient(recipient)
                .setText(text)
                .setSendMode(sendMode)
                .setTemplateKey(null)
                .build();
    }

    private static WebClient webClient(HttpStatus status, String body, String retryAfter) {
        return WebClient.builder()
                .exchangeFunction(request -> {
                    ClientResponse.Builder response = ClientResponse.create(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(body);
                    if (retryAfter != null) {
                        response.header(HttpHeaders.RETRY_AFTER, retryAfter);
                    }
                    return Mono.just(response.build());
                })
                .build();
    }

    private static String successBody() {
        return "{\"messages\":[{\"id\":\"wamid.abc123\"}]}";
    }
}
