package hn.shadowcore.mercadox.email.exception;

/**
 * 4xx response from the Meta Cloud API.
 * Non-retryable — the same request will never succeed without a fix
 * (invalid phone number, expired token, bad template name, etc.).
 * Routes straight to DLT.
 */
public class WhatsAppClientException extends WhatsAppApiException {

    public WhatsAppClientException(int statusCode, String responseBody) {
        super(statusCode, responseBody);
    }
}
