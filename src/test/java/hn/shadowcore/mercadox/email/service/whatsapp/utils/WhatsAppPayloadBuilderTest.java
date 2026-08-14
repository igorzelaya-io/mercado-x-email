package hn.shadowcore.mercadox.email.service.whatsapp.utils;

import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.response.dto.NotificationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class WhatsAppPayloadBuilderTest {

    @Test
    void buildsPayloadWithParametersInTemplateVariableOrder() {
        NotificationTemplate template = NotificationTemplate.builder()
                .whatsappTemplateName("lead_welcome")
                .languageCode("es_HN")
                .variables(List.of("orgName", "userName"))
                .build();

        NotificationRequest request = NotificationRequest.builder()
                .phoneNumber("+50499998888")
                .variables(Map.of("userName", "Igor", "orgName", "MercadoX"))
                .build();

        Map<String, Object> payload = WhatsAppPayloadBuilder.build(template, request);

        assertThat(payload)
                .containsEntry("messaging_product", "whatsapp")
                .containsEntry("to", "+50499998888")
                .containsEntry("type", "template");

        Map<String, Object> templateObject = (Map<String, Object>) payload.get("template");
        assertThat(templateObject.get("name")).isEqualTo("lead_welcome");
        assertThat(((Map<String, String>) templateObject.get("language")).get("code")).isEqualTo("es_HN");

        List<Map<String, Object>> components = (List<Map<String, Object>>) templateObject.get("components");
        Map<String, Object> bodyComponent = components.get(0);
        assertThat(bodyComponent.get("type")).isEqualTo("body");

        List<Map<String, String>> parameters = (List<Map<String, String>>) bodyComponent.get("parameters");
        // template.variables() order is [orgName, userName] -> parameters must follow that order,
        // not the (undefined) iteration order of the request's variables map.
        assertThat(parameters).extracting(p -> p.get("text"))
                .containsExactly("MercadoX", "Igor");
    }

    @Test
    void missingVariableInRequestDefaultsToBlankText() {
        NotificationTemplate template = NotificationTemplate.builder()
                .whatsappTemplateName("lead_welcome")
                .languageCode("en_US")
                .variables(List.of("userName"))
                .build();

        NotificationRequest request = NotificationRequest.builder()
                .phoneNumber("+50499998888")
                .variables(Map.of())
                .build();

        Map<String, Object> payload = WhatsAppPayloadBuilder.build(template, request);

        Map<String, Object> templateObject = (Map<String, Object>) payload.get("template");
        List<Map<String, Object>> components = (List<Map<String, Object>>) templateObject.get("components");
        List<Map<String, String>> parameters = (List<Map<String, String>>) components.get(0).get("parameters");

        assertThat(parameters).hasSize(1);
        assertThat(parameters.get(0)).containsEntry("text", "");
    }
}
