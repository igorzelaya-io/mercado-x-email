package hn.shadowcore.mercadox.email.service.whatsapp;

import hn.shadowcore.mercadox.email.config.WhatsAppWebClientFactory;
import hn.shadowcore.mercadox.email.exception.WhatsAppClientException;
import hn.shadowcore.mercadox.email.exception.WhatsAppServerException;
import hn.shadowcore.mercadox.email.service.NotificationTemplateService;
import hn.shadowcore.mercadox.email.service.whatsapp.utils.WhatsAppPayloadBuilder;
import hn.shadowcore.mercadox.library.entity.model.ai.OrganizationWhatsAppConfig;
import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.model.enums.TemplateChannel;
import hn.shadowcore.mercadox.library.entity.response.dto.NotificationRequest;
import hn.shadowcore.mercadox.library.jpa.repository.OrganizationWhatsAppConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class WhatsAppNotificationService {

    private final WhatsAppWebClientFactory webClientFactory;
    private final OrganizationWhatsAppConfigRepository configRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final List<AbstractWhatsAppNotificationHandler<?>> handlers;

    public WhatsAppNotificationService(
            WhatsAppWebClientFactory webClientFactory,
            OrganizationWhatsAppConfigRepository configRepository,
            NotificationTemplateService notificationTemplateService,
            List<AbstractWhatsAppNotificationHandler<?>> handlers) {
        this.webClientFactory = webClientFactory;
        this.configRepository = configRepository;
        this.notificationTemplateService = notificationTemplateService;
        this.handlers = handlers;
    }

    private static final TemplateChannel TEMPLATE_CHANNEL = TemplateChannel.WHATSAPP;

    public void handle(SpecificRecord event) {

        final AbstractWhatsAppNotificationHandler handler = handlers.stream()
                .filter(h -> h.eventType().equals(event.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(" No WhatsApp handler found for event: "
                        + event.getClass().getSimpleName()));

        final NotificationRequest request = handler.buildRequest(event);

        final OrganizationWhatsAppConfig tenantConfig = configRepository
                .findByOrganizationId(UUID.fromString(request.getOrgId()))
                .orElseThrow(() -> new IllegalStateException(
                        "No WhatsApp configuration found for organization: " + request.getOrgId()));

        final WebClient webClient = webClientFactory.forTenant(tenantConfig);

        final NotificationTemplate template = notificationTemplateService
                .findByNameAndChannel(request.getTemplateKey(), TEMPLATE_CHANNEL);

        Map<String, Object> payload = WhatsAppPayloadBuilder.build(template, request);

        WhatsAppMessageResponse response = webClient.post()
                .uri("/messages")
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new WhatsAppClientException(resp.statusCode().value(), body)))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new WhatsAppServerException(resp.statusCode().value(), body)))
                .bodyToMono(WhatsAppMessageResponse.class)
                .block(Duration.ofSeconds(15));

        if (response == null || response.messages() == null || response.messages().isEmpty()) {
            final String message = "WhatsApp API did not confirm delivery for recipient: " + request.getPhoneNumber();
            log.error(message);
            throw new IllegalStateException(message);
        }

    }


}
