package hn.shadowcore.mercadox.email.listener;

import hn.shadowcore.mercadox.context.utils.annotations.KafkaIdempotent;
import hn.shadowcore.mercadox.email.exception.WhatsAppClientException;
import hn.shadowcore.mercadox.email.exception.WhatsAppServerException;
import hn.shadowcore.mercadox.email.service.NotificationTemplateService;
import hn.shadowcore.mercadox.email.service.whatsapp.WhatsAppFreeformService;
import hn.shadowcore.mercadox.email.service.whatsapp.WhatsAppMessageResponse;
import hn.shadowcore.mercadox.email.service.whatsapp.utils.WhatsAppPayloadBuilder;
import hn.shadowcore.mercadox.library.entity.avro.AiReplyGeneratedEvent;
import hn.shadowcore.mercadox.library.entity.kafka.KafkaTopic;
import hn.shadowcore.mercadox.library.entity.model.ai.OrganizationWhatsAppConfig;
import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.model.enums.TemplateChannel;
import hn.shadowcore.mercadox.library.entity.response.dto.NotificationRequest;
import hn.shadowcore.mercadox.library.jpa.repository.OrganizationWhatsAppConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class AiReplyConsumerListener {

    private final WhatsAppFreeformService freeformService;
    private final OrganizationWhatsAppConfigRepository configRepository;
    private final NotificationTemplateService templateService;
    private final WebClient webClient;

    public AiReplyConsumerListener(
            WhatsAppFreeformService freeformService,
            OrganizationWhatsAppConfigRepository configRepository,
            NotificationTemplateService templateService,
            @Qualifier("whatsAppWebClient") WebClient webClient
    ) {
        this.freeformService = freeformService;
        this.configRepository = configRepository;
        this.templateService = templateService;
        this.webClient = webClient;
    }

    @KafkaIdempotent
    @KafkaListener(topics = KafkaTopic.AI_REPLY_GENERATED, groupId = "whatsapp-service-group")
    public void handleAiReply(ConsumerRecord<String, AiReplyGeneratedEvent> record) {
        AiReplyGeneratedEvent event = record.value();

        String sendMode  = event.getSendMode()  != null ? event.getSendMode() : "";
        String recipient = event.getRecipient() != null ? event.getRecipient() : "";
        String text      = event.getText()      != null ? event.getText() : "";
        String orgId     = event.getOrgId()     != null ? event.getOrgId() : "";

        if (recipient.isBlank()) {
            log.warn("AiReplyGeneratedEvent missing recipient — eventId={}", event.getEventId());
            return;
        }

        if ("FREEFORM".equals(sendMode)) {
            log.info("Sending freeform AI reply orgId={} recipient={}", orgId, recipient);
            freeformService.sendText(recipient, text);
            return;
        }

        // TEMPLATE — outside 24-hour window; send the org's default re-engagement template
        sendReengagementTemplate(orgId, recipient);
    }

    private void sendReengagementTemplate(String orgId, String recipient) {
        Optional<OrganizationWhatsAppConfig> configOpt =
                configRepository.findByOrganizationId(UUID.fromString(orgId));

        if (configOpt.isEmpty() || configOpt.get().getDefaultReengagementTemplate() == null) {
            log.warn("No reengagement template configured for orgId={} — dropping TEMPLATE reply", orgId);
            return;
        }

        String templateName = configOpt.get().getDefaultReengagementTemplate();

        NotificationTemplate template =
                templateService.findByNameAndChannel(templateName, TemplateChannel.WHATSAPP);

        NotificationRequest request = NotificationRequest.builder()
                .phoneNumber(recipient)
                .templateKey(templateName)
                .variables(Map.of())
                .build();

        Map<String, Object> payload = WhatsAppPayloadBuilder.build(template, request);

        WhatsAppMessageResponse response = webClient.post()
                .uri("/messages")
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new WhatsAppClientException(resp.statusCode().value(), body)))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new WhatsAppServerException(resp.statusCode().value(), body)))
                .bodyToMono(WhatsAppMessageResponse.class)
                .block(Duration.ofSeconds(15));

        if (response == null || response.messages() == null || response.messages().isEmpty()) {
            throw new IllegalStateException(
                    "WhatsApp API did not confirm template delivery for recipient: " + recipient);
        }

        log.info("Reengagement template '{}' sent orgId={} recipient={}", templateName, orgId, recipient);
    }
}
