package hn.shadowcore.mercadox.email.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mercadoXEmailOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MercadoX Email API")
                        .description("Notification templates and WhatsApp webhook endpoints")
                        .version("v1"));
    }

}
