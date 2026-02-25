package hn.shadowcore.mercadox.email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@EnableCaching
@SpringBootApplication
@ComponentScan(basePackages={
        "hn.shadowcore.mercadoxoauth",
        "hn.shadowcore.mercadoxlibrary",
        "hn.shadowcore.mercadoxcontext"
})
@EntityScan(basePackages = "hn.shadowcore.mercadoxlibrary")
public class MercadoXEmailApplication {

    public static void main(String[] args) {
        SpringApplication.run(MercadoXEmailApplication.class, args);
    }

}
