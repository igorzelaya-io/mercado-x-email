package hn.shadowcore.mercadox.email.listener;

import hn.shadowcore.mercadox.context.utils.annotations.KafkaIdempotent;
import hn.shadowcore.mercadox.context.utils.annotations.KafkaOrgIdPropagated;
import hn.shadowcore.mercadox.email.service.mailer.EmailOrchestratorService;
import hn.shadowcore.mercadox.library.entity.avro.OrderEmailEvent;
import hn.shadowcore.mercadox.library.entity.avro.UserRegistrationEmailEvent;
import hn.shadowcore.mercadox.library.entity.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailOrchestratorService orchestrator;

    @KafkaIdempotent
    @KafkaOrgIdPropagated
    @KafkaListener(topics = KafkaTopic.USER_REGISTRATION, groupId = "email-service-group")
    public void handleUserRegistered(ConsumerRecord<String, UserRegistrationEmailEvent> record) {
        UserRegistrationEmailEvent event = record.value();
        orchestrator.sendToRecipients(
                event.getEmailTemplate().name(),
                event.getEventSubject(),
                event.getRecipients(),
                event.getPayload()
        );
    }

    @KafkaIdempotent
    @KafkaOrgIdPropagated
    @KafkaListener(topics = KafkaTopic.ORDER_PLACING, groupId = "email-service-group")
    public void handleOrderPlaced(ConsumerRecord<String, OrderEmailEvent> record) {
        sendOrderEmail(record.value());
    }

    @KafkaIdempotent
    @KafkaOrgIdPropagated
    @KafkaListener(topics = KafkaTopic.ORDER_CONFIRMED, groupId = "email-service-group")
    public void handleOrderConfirmed(ConsumerRecord<String, OrderEmailEvent> record) {
        sendOrderEmail(record.value());
    }

    @KafkaIdempotent
    @KafkaOrgIdPropagated
    @KafkaListener(topics = KafkaTopic.ORDER_CANCELLED, groupId = "email-service-group")
    public void handleOrderCancelled(ConsumerRecord<String, OrderEmailEvent> record) {
        sendOrderEmail(record.value());
    }

    private void sendOrderEmail(OrderEmailEvent event) {
        orchestrator.sendToRecipients(
                event.getEmailTemplate().name(),
                event.getEventSubject(),
                event.getRecipients(),
                event.getPayload()
        );
    }
}
