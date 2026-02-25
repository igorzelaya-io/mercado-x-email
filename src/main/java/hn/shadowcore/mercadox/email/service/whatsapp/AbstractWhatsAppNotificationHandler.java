package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadoxlibrary.entity.response.EventDto;
import hn.shadowcore.mercadoxlibrary.entity.response.dto.NotificationRequest;

public interface AbstractWhatsAppNotificationHandler<T extends EventDto> {

    Class<T> eventType();
    String templateKey();
    NotificationRequest buildRequest(T event);

}
