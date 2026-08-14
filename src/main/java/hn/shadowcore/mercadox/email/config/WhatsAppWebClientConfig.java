package hn.shadowcore.mercadox.email.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WhatsAppWebClientConfig {

    @Bean
    public WebClient whatsAppWebClient(
            WebClient.Builder builder,
            @Value("${whatsapp.api.base-url}") String baseUrl,
            @Value("${whatsapp.api.token}") String token) {
        return builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }
}
