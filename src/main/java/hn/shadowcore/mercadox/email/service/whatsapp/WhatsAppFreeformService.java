package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.email.exception.WhatsAppClientException;
import hn.shadowcore.mercadox.email.exception.WhatsAppServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class WhatsAppFreeformService {

    private final WebClient webClient;

    public WhatsAppFreeformService(@Qualifier("whatsAppWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public void sendText(String to, String text) {
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", text)
        );

        WhatsAppMessageResponse response = webClient.post()
                .uri("/messages")
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new WhatsAppClientException(resp.statusCode().value(), body)))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new WhatsAppServerException(resp.statusCode().value(), body)))
                .bodyToMono(WhatsAppMessageResponse.class)
                .block(Duration.ofSeconds(15));

        if (response == null || response.messages() == null || response.messages().isEmpty()) {
            throw new IllegalStateException("WhatsApp API did not confirm delivery for recipient: " + to);
        }

        log.info("Freeform message delivered to={}", to);
    }
}
