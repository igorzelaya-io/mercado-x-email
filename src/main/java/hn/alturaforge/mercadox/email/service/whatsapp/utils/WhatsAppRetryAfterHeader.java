package hn.alturaforge.mercadox.email.service.whatsapp.utils;

import org.springframework.web.reactive.function.client.ClientResponse;

public final class WhatsAppRetryAfterHeader {

    private WhatsAppRetryAfterHeader() {
    }

    /** Parses the Retry-After response header (seconds form) off a 429 response, if present. */
    public static Long seconds(ClientResponse response) {
        String value = response.headers().asHttpHeaders().getFirst("Retry-After");
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
