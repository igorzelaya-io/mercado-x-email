package hn.shadowcore.mercadox.email.config;

import hn.shadowcore.mercadox.context.kafka.KafkaErrorHandlerCustomizer;
import hn.shadowcore.mercadox.email.exception.WhatsAppClientException;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppErrorHandlerCustomizer implements KafkaErrorHandlerCustomizer {

    @Override
    public void customize(DefaultErrorHandler errorHandler) {
        // 4xx from Meta API — retrying the same payload will never succeed.
        // Skip backoff entirely and go straight to DLT for inspection.
        errorHandler.addNotRetryableExceptions(WhatsAppClientException.class);
    }
}
