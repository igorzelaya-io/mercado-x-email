package hn.shadowcore.mercadox.email.service.whatsapp.utils;

import hn.shadowcore.mercadoxlibrary.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadoxlibrary.entity.response.dto.NotificationRequest;

import java.util.List;
import java.util.Map;

public final class WhatsAppPayloadBuilder {

    private WhatsAppPayloadBuilder() {}

    public static Map<String, Object> build(
            NotificationTemplate template,
            NotificationRequest request
    ) {

        List<Map<String, String>> parameters =
                template.getVariables().stream()
                        .map(variableName -> Map.of(
                                "type", "text",
                                "text", request.getVariables()
                                        .getOrDefault(variableName, "")
                        ))
                        .toList();

        Map<String, Object> bodyComponent = Map.of(
                "type", "body",
                "parameters", parameters
        );

        Map<String, Object> templateObject = Map.of(
                "name", template.getWhatsappTemplateName(),
                "language", Map.of("code", template.getLanguageCode()),
                "components", List.of(bodyComponent)
        );

        return Map.of(
                "messaging_product", "whatsapp",
                "to", request.getPhoneNumber(),
                "type", "template",
                "template", templateObject
        );
    }
}