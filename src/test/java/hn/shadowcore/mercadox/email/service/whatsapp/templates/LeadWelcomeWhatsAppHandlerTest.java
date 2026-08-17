package hn.shadowcore.mercadox.email.service.whatsapp.templates;

import hn.shadowcore.mercadox.library.entity.avro.LeadCreatedEvent;
import hn.shadowcore.mercadox.library.entity.response.dto.NotificationRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeadWelcomeWhatsAppHandlerTest {

    private final LeadWelcomeWhatsAppHandler handler = new LeadWelcomeWhatsAppHandler();

    @Test
    void eventTypeIsLeadCreatedEvent() {
        assertThat(handler.eventType()).isEqualTo(LeadCreatedEvent.class);
    }

    @Test
    void templateKeyIsLeadCreationTemplate() {
        assertThat(handler.templateKey()).isEqualTo("LEAD_CREATION_TEMPLATE");
    }

    @Test
    void buildRequestMapsEventFieldsIntoNotificationRequest() {
        LeadCreatedEvent event = LeadCreatedEvent.newBuilder()
                .setOrgName("MercadoX")
                .setUserName("Igor")
                .setEmail("igor@example.com")
                .setPhoneNumber("+50499998888")
                .setOrgId("org-123")
                .setEventId(java.util.UUID.randomUUID().toString())
                .setEventType("LEAD_CREATED")
                .setOccurredAt(java.time.Instant.now().toString())
                .build();

        NotificationRequest request = handler.buildRequest(event);

        assertThat(request.getOrgId()).isEqualTo("org-123");
        assertThat(request.getTemplateKey()).isEqualTo("LEAD_CREATION_TEMPLATE");
        assertThat(request.getPhoneNumber()).isEqualTo("+50499998888");
        assertThat(request.getVariables())
                .containsEntry("userName", "Igor")
                .containsEntry("orgName", "MercadoX");
    }
}
