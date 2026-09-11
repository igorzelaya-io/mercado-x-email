package hn.alturaforge.mercadox.email.service.whatsapp.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WhatsAppRetryAfterHeaderTest {

    @Test
    void seconds_parsesAValidRetryAfterHeader() {
        ClientResponse response = clientResponseWithHeader("30");

        assertThat(WhatsAppRetryAfterHeader.seconds(response)).isEqualTo(30L);
    }

    @Test
    void seconds_returnsNullWhenHeaderIsAbsent() {
        ClientResponse response = clientResponseWithHeader(null);

        assertThat(WhatsAppRetryAfterHeader.seconds(response)).isNull();
    }

    @Test
    void seconds_returnsNullWhenHeaderIsNotNumeric() {
        // Retry-After may also be an HTTP-date per RFC 9110 — not handled here, treated as absent
        ClientResponse response = clientResponseWithHeader("Wed, 21 Oct 2026 07:28:00 GMT");

        assertThat(WhatsAppRetryAfterHeader.seconds(response)).isNull();
    }

    private ClientResponse clientResponseWithHeader(String retryAfterValue) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (retryAfterValue != null) {
            httpHeaders.set("Retry-After", retryAfterValue);
        }
        ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
        when(headers.asHttpHeaders()).thenReturn(httpHeaders);

        ClientResponse response = mock(ClientResponse.class);
        when(response.headers()).thenReturn(headers);
        return response;
    }
}
