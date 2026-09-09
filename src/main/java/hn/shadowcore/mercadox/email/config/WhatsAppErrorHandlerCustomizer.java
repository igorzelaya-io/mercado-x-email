package hn.shadowcore.mercadox.email.config;

import hn.shadowcore.mercadox.context.kafka.KafkaErrorHandlerCustomizer;
import hn.shadowcore.mercadox.email.exception.WhatsAppClientException;
import hn.shadowcore.mercadox.email.exception.WhatsAppRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WhatsAppErrorHandlerCustomizer implements KafkaErrorHandlerCustomizer {

    @Override
    public void customize(DefaultErrorHandler errorHandler) {
        // Permanent 4xx from Meta API (bad phone number, expired token, bad template —
        // never a throttling response) — retrying the same payload will never succeed.
        // Skip backoff entirely and go straight to DLT for inspection.
        //
        // WhatsAppRateLimitException (429 / error 130429) is deliberately NOT here — Meta's
        // per-second throughput caps and daily-recipient tiers are transient, so it stays on
        // the default exponential-backoff retry path alongside WhatsAppServerException (5xx).
        errorHandler.addNotRetryableExceptions(WhatsAppClientException.class);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            String retryAfter = cause instanceof WhatsAppRateLimitException rateLimitEx
                    && rateLimitEx.getRetryAfterSeconds() != null
                    ? " retryAfterSeconds=" + rateLimitEx.getRetryAfterSeconds()
                    : "";
            log.warn("Kafka redelivery attempt={} for topic={} partition={} offset={} — {}: {}{}",
                    deliveryAttempt, record.topic(), record.partition(), record.offset(),
                    cause.getClass().getSimpleName(), cause.getMessage(), retryAfter);
        });
    }
}
