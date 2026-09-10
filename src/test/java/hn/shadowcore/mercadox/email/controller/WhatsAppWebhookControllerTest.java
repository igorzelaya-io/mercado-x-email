package hn.shadowcore.mercadox.email.controller;

import hn.shadowcore.mercadox.email.service.whatsapp.WhatsAppPayloadReaderService;
import hn.shadowcore.mercadox.library.entity.request.webhook.WhatsAppWebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WhatsAppWebhookControllerTest {

    private WhatsAppPayloadReaderService payloadReaderService;
    private WhatsAppWebhookController controller;

    @BeforeEach
    void setUp() {
        payloadReaderService = mock(WhatsAppPayloadReaderService.class);
        controller = new WhatsAppWebhookController(payloadReaderService);
        ReflectionTestUtils.setField(controller, "verifyToken", "expected-token");
    }

    @Test
    void matchingSubscribeChallengeIsReturned() {
        var response = controller.verifyWebhook("subscribe", "expected-token", "challenge-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("challenge-123");
    }

    @Test
    void wrongModeOrTokenIsForbidden() {
        assertThat(controller.verifyWebhook("unsubscribe", "expected-token", "challenge").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.verifyWebhook("subscribe", "wrong-token", "challenge").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void inboundMessageIsDelegatedAndAcknowledged() {
        WhatsAppWebhookPayload payload = new WhatsAppWebhookPayload("whatsapp_business_account", null);

        var response = controller.receiveMessage(payload);

        verify(payloadReaderService).read(payload);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
    }
}
