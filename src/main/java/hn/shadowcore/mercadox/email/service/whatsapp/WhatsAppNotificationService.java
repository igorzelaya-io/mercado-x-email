package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.email.service.NotificationTemplateService;
import hn.shadowcore.mercadox.email.service.whatsapp.utils.WhatsAppPayloadBuilder;
import hn.shadowcore.mercadoxlibrary.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadoxlibrary.entity.model.enums.NotificationTemplateName;
import hn.shadowcore.mercadoxlibrary.entity.model.enums.TemplateChannel;
import hn.shadowcore.mercadoxlibrary.entity.model.enums.kafka.event.LeadCreatedEvent;
import hn.shadowcore.mercadoxlibrary.entity.response.EventDto;
import hn.shadowcore.mercadoxlibrary.entity.response.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WhatsAppNotificationService {

    private final WebClient webClient;

    private final NotificationTemplateService notificationTemplateService;

    private final List<AbstractWhatsAppNotificationHandler<?>> handlers;

    private static final TemplateChannel TEMPLATE_CHANNEL = TemplateChannel.WHATSAPP;

    public <T extends EventDto> void handle(T event) {

        final AbstractWhatsAppNotificationHandler handler =handlers.stream()
                .filter(h -> h.eventType().equals(event.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(" No WhatsApp handler found for event: "
                        + event.getClass().getSimpleName()));

        final NotificationRequest request = handler.buildRequest(event);

        final NotificationTemplate template = notificationTemplateService
                .findByNameAndChannel(request.getTemplateKey(), TEMPLATE_CHANNEL);

        Map<String, Object> payload = WhatsAppPayloadBuilder.build(template, request);

        webClient.post()
                .uri("/messages")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();


    }


}
