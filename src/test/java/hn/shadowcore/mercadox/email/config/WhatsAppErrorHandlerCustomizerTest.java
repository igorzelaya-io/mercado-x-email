package hn.shadowcore.mercadox.email.config;

import hn.shadowcore.mercadox.email.exception.WhatsAppClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class WhatsAppErrorHandlerCustomizerTest {

    @Mock
    private DefaultErrorHandler errorHandler;

    @Test
    void customize_marksOnlyWhatsAppClientExceptionAsNotRetryable() {
        new WhatsAppErrorHandlerCustomizer().customize(errorHandler);

        // The only addNotRetryableExceptions call the customizer makes — proves
        // WhatsAppRateLimitException / WhatsAppServerException are never included,
        // since a second call with either would fail verifyNoMoreInteractions below.
        verify(errorHandler).addNotRetryableExceptions(WhatsAppClientException.class);
        verify(errorHandler).setRetryListeners(ArgumentMatchers.any(RetryListener.class));
        verifyNoMoreInteractions(errorHandler);
    }
}
