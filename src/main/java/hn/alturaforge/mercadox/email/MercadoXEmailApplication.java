package hn.alturaforge.mercadox.email;

import hn.alturaforge.mercadox.context.config.JwtConfig;
import hn.alturaforge.mercadox.oauth.service.OAuthTenantValidatorService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableCaching
@SpringBootApplication(scanBasePackages = {
        "hn.alturaforge.mercadox.email",
        "hn.alturaforge.mercadox.context",
        "hn.alturaforge.mercadox.library"
})
@Import({OAuthTenantValidatorService.class, JwtConfig.class})
@EntityScan(basePackages = "hn.alturaforge.mercadox.library")
public class MercadoXEmailApplication {

    public static void main(String[] args) {
        SpringApplication.run(MercadoXEmailApplication.class, args);
    }

}
