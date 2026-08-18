package hn.shadowcore.mercadox.email.config;

import hn.shadowcore.mercadox.context.exception.InvalidEventIdException;
import hn.shadowcore.mercadox.context.kafka.KafkaErrorHandlerCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

@TestConfiguration
public class TestKafkaErrorHandlerConfig {

    /**
     * Overrides the production error handler with zero retries so no retry timers
     * are scheduled between test methods.
     */
    @Bean
    @Primary
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            List<KafkaErrorHandlerCustomizer> customizers) {

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate),
                new FixedBackOff(0L, 0L));

        errorHandler.addNotRetryableExceptions(InvalidEventIdException.class);
        customizers.forEach(c -> c.customize(errorHandler));
        return errorHandler;
    }

    /**
     * Overrides the production container factory with AckMode.RECORD so each message
     * commits its offset immediately after the listener returns — not in a batch.
     *
     * Without this, the default AckMode.BATCH means an offset may be uncommitted when
     * @BeforeEach flushes Redis. If that message is re-read (rebalance / slow commit),
     * its eventId is no longer in Redis, claimProcessing() succeeds, and the service is
     * called in the next test's window, breaking verifyNoInteractions assertions.
     */
    @Bean
    @Primary
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            DefaultErrorHandler kafkaErrorHandler,
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
