package hn.shadowcore.mercadox.email.exception;

/**
 * HTTP 429 from the Meta Cloud API (throttling — e.g. error code 130429).
 * Retryable — Meta enforces per-second throughput caps and tiered daily-recipient
 * limits, so the same request can succeed once the window clears. Deliberately a
 * separate type from {@link WhatsAppClientException} so it is not swept into that
 * class's "all 4xx are permanent" non-retryable classification.
 */
public class WhatsAppRateLimitException extends WhatsAppApiException {

    private final Long retryAfterSeconds;

    public WhatsAppRateLimitException(int statusCode, String responseBody, Long retryAfterSeconds) {
        super(statusCode, responseBody);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Value of the Retry-After response header, if Meta sent one. */
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
