package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.context.utils.CorrelationIdContext;
import hn.shadowcore.mercadox.context.utils.KafkaProducerRecordFactory;
import hn.shadowcore.mercadox.context.utils.OrgIdContextHolder;
import hn.shadowcore.mercadox.library.entity.avro.WhatsAppMessageReceivedEvent;
import hn.shadowcore.mercadox.library.entity.kafka.KafkaTopic;
import hn.shadowcore.mercadox.library.entity.kafka.publisher.WhatsAppEventPublisher;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaWhatsAppEventPublisher implements WhatsAppEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishMessageReceived(WhatsAppMessageReceivedEvent event) {
        try {
            // orgId resolved from phone_number_id — set it so the Kafka header is populated
            // for @KafkaOrgIdPropagated on the consumer side
            OrgIdContextHolder.setTenantId(event.getOrgId());

            // This is the true entry point for a conversation's trace — no upstream Kafka
            // consumer to inherit a correlation ID from. mercado-x-ai adopts this value as
            // the conversation's permanent correlation ID on the first message, or discards
            // it in favor of the already-persisted one on every message after.
            CorrelationIdContext.set(UUID.randomUUID().toString());

            ProducerRecord<String, Object> record = KafkaProducerRecordFactory
                    .buildWithOrgIdHeader(KafkaTopic.WHATSAPP_MESSAGE_RECEIVED, event.getEventId(), event);
            kafkaTemplate.send(record);
        } finally {
            OrgIdContextHolder.clear();
            CorrelationIdContext.clear();
        }
    }
}
