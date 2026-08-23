package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.library.entity.avro.WhatsAppMessageReceivedEvent;
import hn.shadowcore.mercadox.library.entity.kafka.publisher.WhatsAppEventPublisher;
import hn.shadowcore.mercadox.library.entity.model.ai.OrganizationWhatsAppConfig;
import hn.shadowcore.mercadox.library.entity.request.webhook.WhatsAppWebhookPayload;
import hn.shadowcore.mercadox.library.jpa.repository.OrganizationWhatsAppConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the inbound WhatsApp webhook pipeline: validates the Meta envelope,
 * resolves the tenant, dedupes against transport retries, and publishes
 * WhatsAppMessageReceivedEvent onto Kafka. The controller only receives and
 * delegates — all of this used to live in the @PostMapping handler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppPayloadReaderService {

    private final WhatsAppWamidDedupeService wamidDedupeService;
    private final OrganizationWhatsAppConfigRepository configRepository;
    private final WhatsAppEventPublisher eventPublisher;

    public void read(WhatsAppWebhookPayload payload) {
        if (payload.entry() == null) {
            return;
        }

        for (WhatsAppWebhookPayload.Entry entry : payload.entry()) {
            if (entry.changes() == null) continue;

            for (WhatsAppWebhookPayload.Change change : entry.changes()) {
                processChange(change.value());
            }
        }
    }

    private void processChange(WhatsAppWebhookPayload.Value value) {
        if (value == null || isEmpty(value.messages())) {
            // Status update (delivery/read receipt) — not a user message, skip
            return;
        }

        String phoneNumberId = value.metadata() != null
                ? value.metadata().phoneNumberId()
                : null;

        if (phoneNumberId == null) {
            log.warn("Received webhook with no phone_number_id — skipping");
            return;
        }

        Optional<OrganizationWhatsAppConfig> configOpt =
                configRepository.findByPhoneNumberId(phoneNumberId);

        if (configOpt.isEmpty()) {
            log.warn("No OrganizationWhatsAppConfig for phone_number_id={} — skipping", phoneNumberId);
            return;
        }

        OrganizationWhatsAppConfig config = configOpt.get();

        for (WhatsAppWebhookPayload.Message message : value.messages()) {
            processMessage(message, config);
        }
    }

    private void processMessage(
            WhatsAppWebhookPayload.Message message,
            OrganizationWhatsAppConfig config
    ) {
        if (!"text".equals(message.type()) || message.text() == null) {
            log.debug("Skipping non-text message type={} wamid={}", message.type(), message.id());
            return;
        }

        // Layer 1 dedupe — guard against Meta transport retries before assigning an eventId
        if (!wamidDedupeService.claim(message.id())) {
            log.info("Dropped duplicate wamid={}", message.id());
            return;
        }

        WhatsAppMessageReceivedEvent event = WhatsAppMessageReceivedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType("WHATSAPP_MESSAGE_RECEIVED")
                .setOrgId(config.getOrganizationId().toString())
                .setOccurredAt(Instant.now().toString())
                .setWamid(message.id())
                .setPhoneNumberId(config.getPhoneNumberId())
                .setFromPhoneNumber(message.from())
                .setMessageText(message.text().body())
                .setMessageType(message.type())
                .build();

        eventPublisher.publishMessageReceived(event);

        log.info("Published WhatsAppMessageReceivedEvent orgId={} from={} wamid={}",
                config.getOrganizationId(), message.from(), message.id());
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
