package hn.shadowcore.mercadox.email.listener;

import hn.shadowcore.mercadox.email.service.mailer.EmailOrchestratorService;
import hn.shadowcore.mercadoxcontext.utils.annotations.KafkaIdempotent;
import hn.shadowcore.mercadoxcontext.utils.annotations.KafkaOrgIdPropagated;
import hn.shadowcore.mercadoxlibrary.entity.model.enums.kafka.KafkaTopic;
import hn.shadowcore.mercadoxlibrary.entity.response.EventDto;
import hn.shadowcore.mercadoxlibrary.entity.response.dto.EmailEventDto;
import hn.shadowcore.mercadoxlibrary.entity.response.dto.OrderDto;
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
    public void handleUserRegistered(ConsumerRecord<String, EventDto<EmailEventDto<String>>> emailEventDto) {
        orchestrator.sendToMultipleRecipients(emailEventDto.value().getEventPayload(),
                emailEventDto.value().getEventPayload().getEmailTemplate().name());
    }
    
    @KafkaIdempotent
    @KafkaOrgIdPropagated
    @KafkaListener(topics = KafkaTopic.ORDER_PLACING, groupId = "email-service-group")
    public void handleOrderPlaced(ConsumerRecord<String, EventDto<EmailEventDto<OrderDto>>> eventDto) {
        orchestrator.sendToMultipleRecipients(eventDto.value().getEventPayload(),
                eventDto.value().getEventPayload().getEmailTemplate().name());
    }

    @KafkaIdempotent
    @KafkaOrgIdPropagated
    @KafkaListener(topics = KafkaTopic.ORDER_CONFIRMED, groupId = "email-service-group")
    public void handlerOrderConfirmed(ConsumerRecord<String, EventDto<EmailEventDto<OrderDto>>> eventDto) {
        orchestrator.sendToMultipleRecipients(eventDto.value().getEventPayload(),
                eventDto.value().getEventPayload().getEmailTemplate().name());
    }

    @KafkaIdempotent
    @KafkaOrgIdPropagated
    @KafkaListener(topics = KafkaTopic.ORDER_CANCELLED, groupId = "email-service-group")
    public void handleOrderCancelled(ConsumerRecord<String, EventDto<EmailEventDto<OrderDto>>> eventDto) {
        orchestrator.sendToMultipleRecipients(eventDto.value().getEventPayload(),
                eventDto.value().getEventPayload().getEmailTemplate().name());
    }

}
