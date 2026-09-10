package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.library.entity.avro.WhatsAppMessageReceivedEvent;
import hn.shadowcore.mercadox.library.entity.kafka.publisher.WhatsAppEventPublisher;
import hn.shadowcore.mercadox.library.entity.model.ai.OrganizationWhatsAppConfig;
import hn.shadowcore.mercadox.library.entity.request.webhook.WhatsAppWebhookPayload;
import hn.shadowcore.mercadox.library.jpa.repository.OrganizationWhatsAppConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppPayloadReaderServiceTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private WhatsAppWamidDedupeService dedupeService;

    @Mock
    private OrganizationWhatsAppConfigRepository configRepository;

    @Mock
    private WhatsAppEventPublisher eventPublisher;

    @Test
    void nullEntryAndNullChangesAreIgnored() {
        reader().read(new WhatsAppWebhookPayload("whatsapp_business_account", null));
        reader().read(new WhatsAppWebhookPayload("whatsapp_business_account",
                List.of(new WhatsAppWebhookPayload.Entry("entry", null))));

        verifyNoInteractions(dedupeService, configRepository, eventPublisher);
    }

    @Test
    void statusOnlyAndNullChangeValuesAreIgnored() {
        WhatsAppWebhookPayload.Value statusOnly = new WhatsAppWebhookPayload.Value(
                "whatsapp", null, List.of(), List.of(new WhatsAppWebhookPayload.Status(
                "wamid-status", "read", "123", "+50499990000")));

        reader().read(payload(List.of(
                new WhatsAppWebhookPayload.Change(null, "messages"),
                new WhatsAppWebhookPayload.Change(statusOnly, "messages"))));

        verifyNoInteractions(dedupeService, configRepository, eventPublisher);
    }

    @Test
    void missingPhoneNumberOrTenantConfigurationIsIgnored() {
        WhatsAppWebhookPayload.Message message = textMessage("wamid-1", "hello");
        WhatsAppWebhookPayload.Value noMetadata = value(null, List.of(message));
        WhatsAppWebhookPayload.Value noPhoneId = value(new WhatsAppWebhookPayload.Metadata("+504", null),
                List.of(message));
        WhatsAppWebhookPayload.Value unknownPhone = value(
                new WhatsAppWebhookPayload.Metadata("+504", "unknown"), List.of(message));
        when(configRepository.findByPhoneNumberId("unknown")).thenReturn(Optional.empty());

        reader().read(payload(List.of(
                new WhatsAppWebhookPayload.Change(noMetadata, "messages"),
                new WhatsAppWebhookPayload.Change(noPhoneId, "messages"),
                new WhatsAppWebhookPayload.Change(unknownPhone, "messages"))));

        verify(configRepository).findByPhoneNumberId("unknown");
        verifyNoInteractions(dedupeService, eventPublisher);
    }

    @Test
    void nonTextNullTextAndDuplicateMessagesAreNotPublished() {
        OrganizationWhatsAppConfig config = config();
        when(configRepository.findByPhoneNumberId("phone-id")).thenReturn(Optional.of(config));
        when(dedupeService.claim("duplicate")).thenReturn(false);
        List<WhatsAppWebhookPayload.Message> messages = List.of(
                new WhatsAppWebhookPayload.Message("image", "+5041", "123", "image", null),
                new WhatsAppWebhookPayload.Message("null-text", "+5042", "124", "text", null),
                textMessage("duplicate", "hello"));

        reader().read(payload(List.of(new WhatsAppWebhookPayload.Change(
                value(new WhatsAppWebhookPayload.Metadata("+504", "phone-id"), messages), "messages"))));

        verify(dedupeService).claim("duplicate");
        verify(eventPublisher, never()).publishMessageReceived(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validTextMessagesArePublishedWithResolvedTenantContext() {
        OrganizationWhatsAppConfig config = config();
        when(configRepository.findByPhoneNumberId("phone-id")).thenReturn(Optional.of(config));
        when(dedupeService.claim("wamid-1")).thenReturn(true);
        when(dedupeService.claim("wamid-2")).thenReturn(true);

        reader().read(payload(List.of(new WhatsAppWebhookPayload.Change(
                value(new WhatsAppWebhookPayload.Metadata("+504", "phone-id"), List.of(
                        textMessage("wamid-1", "hello"), textMessage("wamid-2", "world"))),
                "messages"))));

        ArgumentCaptor<WhatsAppMessageReceivedEvent> captor =
                ArgumentCaptor.forClass(WhatsAppMessageReceivedEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishMessageReceived(captor.capture());
        WhatsAppMessageReceivedEvent first = captor.getAllValues().get(0);
        assertThat(first.getEventId().toString()).isNotBlank();
        assertThat(first.getEventType().toString()).isEqualTo("WHATSAPP_MESSAGE_RECEIVED");
        assertThat(first.getOrgId().toString()).isEqualTo(ORG_ID.toString());
        assertThat(first.getPhoneNumberId().toString()).isEqualTo("phone-id");
        assertThat(first.getFromPhoneNumber().toString()).isEqualTo("+50499990000");
        assertThat(first.getWamid().toString()).isEqualTo("wamid-1");
        assertThat(first.getMessageText().toString()).isEqualTo("hello");
        assertThat(first.getMessageType().toString()).isEqualTo("text");
        assertThat(first.getOccurredAt().toString()).isNotBlank();
    }

    private WhatsAppPayloadReaderService reader() {
        return new WhatsAppPayloadReaderService(dedupeService, configRepository, eventPublisher);
    }

    private OrganizationWhatsAppConfig config() {
        return OrganizationWhatsAppConfig.create(ORG_ID, "phone-id", "waba-id", "token", "template");
    }

    private WhatsAppWebhookPayload payload(List<WhatsAppWebhookPayload.Change> changes) {
        return new WhatsAppWebhookPayload("whatsapp_business_account",
                List.of(new WhatsAppWebhookPayload.Entry("entry", changes)));
    }

    private WhatsAppWebhookPayload.Value value(
            WhatsAppWebhookPayload.Metadata metadata,
            List<WhatsAppWebhookPayload.Message> messages
    ) {
        return new WhatsAppWebhookPayload.Value("whatsapp", metadata, messages, null);
    }

    private WhatsAppWebhookPayload.Message textMessage(String id, String body) {
        return new WhatsAppWebhookPayload.Message(
                id, "+50499990000", "123", "text", new WhatsAppWebhookPayload.Text(body));
    }
}
