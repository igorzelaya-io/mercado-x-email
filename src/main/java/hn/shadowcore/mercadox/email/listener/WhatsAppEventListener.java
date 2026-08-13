package hn.shadowcore.mercadox.email.listener;


import hn.shadowcore.mercadox.context.utils.annotations.KafkaIdempotent;
import hn.shadowcore.mercadox.email.service.whatsapp.WhatsAppNotificationService;
import hn.shadowcore.mercadox.library.entity.model.enums.kafka.KafkaTopic;
import hn.shadowcore.mercadox.library.entity.model.enums.kafka.event.LeadCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WhatsAppEventListener {

    private final WhatsAppNotificationService whatsAppNotificationService;

    @KafkaIdempotent
    @KafkaListener(topics = KafkaTopic.LEAD_CREATED, groupId = "whatsapp-service-group")
    public void handleLeadCreation(ConsumerRecord<String, LeadCreatedEvent> event) {
        whatsAppNotificationService.handle(event.value());
    }

}