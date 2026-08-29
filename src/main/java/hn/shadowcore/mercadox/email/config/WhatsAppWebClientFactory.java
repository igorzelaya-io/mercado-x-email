package hn.shadowcore.mercadox.email.config;

import hn.shadowcore.mercadox.library.entity.model.ai.OrganizationWhatsAppConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WhatsAppWebClientFactory {

    private final WebClient.Builder builder;
    private final String graphBaseUrl;

    public WhatsAppWebClientFactory(
            WebClient.Builder builder,
            @Value("${whatsapp.api.graph-base-url}") String graphBaseUrl) {
        this.builder = builder;
        this.graphBaseUrl = graphBaseUrl;
    }

    public WebClient forTenant(OrganizationWhatsAppConfig config) {
        return builder.clone()
                .baseUrl(graphBaseUrl + "/" + config.getPhoneNumberId())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAccessToken())
                .build();
    }
}
