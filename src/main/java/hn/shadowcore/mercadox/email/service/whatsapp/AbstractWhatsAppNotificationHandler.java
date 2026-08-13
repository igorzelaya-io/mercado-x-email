package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.library.entity.model.enums.kafka.event.DomainEvent;
import hn.shadowcore.mercadox.library.entity.response.dto.NotificationRequest;

public interface AbstractWhatsAppNotificationHandler<T extends DomainEvent> {

    Class<T> eventType();
    String templateKey();
    NotificationRequest buildRequest(T event);

}
