package hn.shadowcore.mercadox.email.exception;

/**
 * 5xx response from the Meta Cloud API.
 * Retryable — transient server-side failure that may resolve on retry.
 * Goes through exponential backoff before landing in DLT.
 */
public class WhatsAppServerException extends WhatsAppApiException {

    public WhatsAppServerException(int statusCode, String responseBody) {
        super(statusCode, responseBody);
    }
}
