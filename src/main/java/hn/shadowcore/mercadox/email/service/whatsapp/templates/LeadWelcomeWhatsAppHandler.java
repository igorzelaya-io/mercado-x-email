package hn.shadowcore.mercadox.email.service.whatsapp.templates;

import hn.shadowcore.mercadox.email.service.whatsapp.AbstractWhatsAppNotificationHandler;
import hn.shadowcore.mercadox.library.entity.avro.LeadCreatedEvent;
import hn.shadowcore.mercadox.library.entity.model.enums.NotificationTemplateName;
import hn.shadowcore.mercadox.library.entity.response.dto.NotificationRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LeadWelcomeWhatsAppHandler
        implements AbstractWhatsAppNotificationHandler<LeadCreatedEvent> {

    @Override
    public Class<LeadCreatedEvent> eventType() {
        return LeadCreatedEvent.class;
    }

    @Override
    public String templateKey() {
        return NotificationTemplateName.LEAD_CREATION_TEMPLATE.name();
    }

    @Override
    public NotificationRequest buildRequest(LeadCreatedEvent event) {
        return new NotificationRequest(
                event.getOrgId(),
                templateKey(),
                event.getPhoneNumber(),
                Map.of(
                        "userName", event.getUserName(),
                        "orgName", event.getOrgName()
                )
        );
    }
}
