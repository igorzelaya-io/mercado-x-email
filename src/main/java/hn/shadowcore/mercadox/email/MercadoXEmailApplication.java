package hn.shadowcore.mercadox.email;

import hn.shadowcore.mercadox.context.config.JwtConfig;
import hn.shadowcore.mercadox.oauth.service.OAuthTenantValidatorService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableCaching
@SpringBootApplication
@Import({OAuthTenantValidatorService.class, JwtConfig.class})
@EntityScan(basePackages = "hn.shadowcore.mercadox.library")
public class MercadoXEmailApplication {

    public static void main(String[] args) {
        SpringApplication.run(MercadoXEmailApplication.class, args);
    }

}
