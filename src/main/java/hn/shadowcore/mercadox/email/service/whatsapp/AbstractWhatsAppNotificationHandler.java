package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.library.entity.response.dto.NotificationRequest;
import org.apache.avro.specific.SpecificRecord;

public interface AbstractWhatsAppNotificationHandler<T extends SpecificRecord> {

    Class<T> eventType();
    String templateKey();
    NotificationRequest buildRequest(T event);

}
