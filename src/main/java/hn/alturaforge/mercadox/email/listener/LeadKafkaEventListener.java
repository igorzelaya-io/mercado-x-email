package hn.alturaforge.mercadox.email.listener;

import hn.alturaforge.mercadox.context.utils.annotations.KafkaIdempotent;
import hn.alturaforge.mercadox.email.service.whatsapp.WhatsAppNotificationService;
import hn.alturaforge.mercadox.library.entity.avro.LeadCreatedEvent;
import hn.alturaforge.mercadox.library.entity.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeadKafkaEventListener {

    private final WhatsAppNotificationService whatsAppNotificationService;

    @KafkaIdempotent
    @KafkaListener(topics = KafkaTopic.LEAD_CREATED, groupId = "whatsapp-service-group")
    public void handleLeadCreation(ConsumerRecord<String, LeadCreatedEvent> record) {
        whatsAppNotificationService.handle(record.value());
    }
}