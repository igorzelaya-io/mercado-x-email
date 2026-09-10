package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.email.exception.WhatsAppClientException;
import hn.shadowcore.mercadox.email.exception.WhatsAppRateLimitException;
import hn.shadowcore.mercadox.email.exception.WhatsAppServerException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsAppFreeformServiceTest {

    @Test
    void sendText_completesWhenMetaConfirmsDelivery() {
        WhatsAppFreeformService service = service(HttpStatus.OK, successBody(), null);

        assertThatCode(() -> service.sendText("+50499998888", "hello")).doesNotThrowAnyException();
    }

    @Test
    void sendText_rejectsResponseWithoutDeliveryConfirmation() {
        WhatsAppFreeformService service = service(HttpStatus.OK, "{\"messages\":[]}", null);

        assertThatThrownBy(() -> service.sendText("+50499998888", "hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("+50499998888");
    }

    @Test
    void sendText_mapsRateLimitResponseAndRetryAfterHeader() {
        WhatsAppFreeformService service = service(HttpStatus.TOO_MANY_REQUESTS, "throttled", "45");

        assertThatThrownBy(() -> service.sendText("+50499998888", "hello"))
                .isInstanceOfSatisfying(WhatsAppRateLimitException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(429);
                    assertThat(exception.getResponseBody()).isEqualTo("throttled");
                    assertThat(exception.getRetryAfterSeconds()).isEqualTo(45L);
                });
    }

    @Test
    void sendText_mapsPermanentClientFailure() {
        WhatsAppFreeformService service = service(HttpStatus.BAD_REQUEST, "bad recipient", null);

        assertThatThrownBy(() -> service.sendText("+50499998888", "hello"))
                .isInstanceOf(WhatsAppClientException.class)
                .hasMessageContaining("400");
    }

    @Test
    void sendText_mapsServerFailure() {
        WhatsAppFreeformService service = service(HttpStatus.SERVICE_UNAVAILABLE, "try later", null);

        assertThatThrownBy(() -> service.sendText("+50499998888", "hello"))
                .isInstanceOf(WhatsAppServerException.class)
                .hasMessageContaining("503");
    }

    private static WhatsAppFreeformService service(HttpStatus status, String body, String retryAfter) {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> {
                    ClientResponse.Builder response = ClientResponse.create(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(body);
                    if (retryAfter != null) {
                        response.header(HttpHeaders.RETRY_AFTER, retryAfter);
                    }
                    return Mono.just(response.build());
                })
                .build();
        return new WhatsAppFreeformService(client);
    }

    private static String successBody() {
        return "{\"messages\":[{\"id\":\"wamid.abc123\"}]}";
    }
}
