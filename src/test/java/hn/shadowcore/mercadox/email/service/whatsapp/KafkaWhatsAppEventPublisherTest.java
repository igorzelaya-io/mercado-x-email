package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.context.utils.CorrelationIdContext;
import hn.shadowcore.mercadox.context.utils.OrgIdContextHolder;
import hn.shadowcore.mercadox.library.entity.avro.WhatsAppMessageReceivedEvent;
import hn.shadowcore.mercadox.library.entity.kafka.KafkaTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaWhatsAppEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void publishMessageReceived_addsTenantAndCorrelationHeadersThenClearsContexts() {
        WhatsAppMessageReceivedEvent event = event();
        KafkaWhatsAppEventPublisher publisher = new KafkaWhatsAppEventPublisher(kafkaTemplate);

        publisher.publishMessageReceived(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, Object> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(KafkaTopic.WHATSAPP_MESSAGE_RECEIVED);
        assertThat(record.key()).isEqualTo(event.getEventId());
        assertThat(record.value()).isSameAs(event);
        assertThat(header(record, "x-org-id")).isEqualTo(event.getOrgId());
        assertThat(header(record, "x-correlation-id")).isNotBlank();
        assertThat(OrgIdContextHolder.getTenantId()).isNull();
        assertThat(CorrelationIdContext.get()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishMessageReceived_clearsContextsWhenKafkaSendFails() {
        WhatsAppMessageReceivedEvent event = event();
        RuntimeException failure = new RuntimeException("Kafka unavailable");
        doThrow(failure).when(kafkaTemplate).send(org.mockito.ArgumentMatchers.any(ProducerRecord.class));

        assertThatThrownBy(() -> new KafkaWhatsAppEventPublisher(kafkaTemplate)
                .publishMessageReceived(event)).isSameAs(failure);
        assertThat(OrgIdContextHolder.getTenantId()).isNull();
        assertThat(CorrelationIdContext.get()).isNull();
    }

    private static String header(ProducerRecord<String, Object> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    private static WhatsAppMessageReceivedEvent event() {
        return WhatsAppMessageReceivedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType("WHATSAPP_MESSAGE_RECEIVED")
                .setOrgId(UUID.randomUUID().toString())
                .setOccurredAt(Instant.now().toString())
                .setWamid("wamid.abc123")
                .setPhoneNumberId("phone-id")
                .setFromPhoneNumber("+50499998888")
                .setMessageText("hello")
                .setMessageType("text")
                .build();
    }
}
