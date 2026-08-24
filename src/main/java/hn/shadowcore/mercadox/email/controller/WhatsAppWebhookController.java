package hn.shadowcore.mercadox.email.controller;

import hn.shadowcore.mercadox.email.service.whatsapp.WhatsAppPayloadReaderService;
import hn.shadowcore.mercadox.library.entity.request.webhook.WhatsAppWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin translation layer between Meta and the internal pipeline — no AI logic here.
 *
 * GET  /webhook — responds to Meta's one-time hub verification challenge.
 * POST /webhook — receives inbound WhatsApp messages; HMAC already verified by
 *                 WhatsAppSignatureVerificationFilter before this method runs.
 *                 Validation, dedupe and Kafka publishing live in WhatsAppPayloadReaderService.
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppPayloadReaderService payloadReaderService;

    @Value("${whatsapp.api.verify-token}")
    private String verifyToken;

    // -----------------------------------------------------------------------
    // GET — hub challenge (no HMAC on this request)
    // -----------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge
    ) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Webhook verification failed — token mismatch or wrong mode");
        return ResponseEntity.status(403).build();
    }

    // -----------------------------------------------------------------------
    // POST — inbound messages (HMAC verified upstream by filter)
    // -----------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody WhatsAppWebhookPayload payload) {
        payloadReaderService.read(payload);

        // Always return 200 — Meta stops retrying on any 2xx
        return ResponseEntity.ok().build();
    }
}
