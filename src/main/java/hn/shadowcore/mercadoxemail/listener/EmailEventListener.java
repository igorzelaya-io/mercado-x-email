package hn.shadowcore.mercadoxemail.listener;

import hn.shadowcore.mercadoxemail.service.EmailOrchestrator;
import hn.shadowcore.mercadoxemail.util.KafkaIdempotent;
import hn.shadowcore.mercadoxlibrary.entity.model.enums.EmailTemplate;
import hn.shadowcore.mercadoxlibrary.entity.model.enums.KafkaTopic;
import hn.shadowcore.mercadoxlibrary.entity.response.dto.EmailEventDto;
import hn.shadowcore.mercadoxlibrary.entity.response.dto.OrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailOrchestrator orchestrator;
    @KafkaIdempotent
    @KafkaListener(topics = KafkaTopic.USER_REGISTRATION, groupId = "email-service-group")
    public void handleUserRegistered(EmailEventDto<String> emailEventDto) {
        orchestrator.sendToMultipleRecipients(emailEventDto, EmailTemplate.USER_VALIDATION_TEMPLATE.getValue());
    }
    @KafkaIdempotent
    @KafkaListener(topics = KafkaTopic.ORDER_PLACING, groupId = "email-service-group")
    public void handleOrderPlaced(EmailEventDto<OrderDto> eventDto) {
        orchestrator.sendToMultipleRecipients(eventDto, EmailTemplate.ORDER_REQUEST_TEMPLATE.getValue());
    }

    @KafkaIdempotent
    @KafkaListener(topics = KafkaTopic.ORDER_CONFIRMED, groupId = "email-service-group")
    public void handlerOrderPlaced(EmailEventDto<OrderDto> eventDto) {
        orchestrator.sendToMultipleRecipients(eventDto, EmailTemplate.);
    }

}
