package hn.shadowcore.mercadox.email.config;

import hn.shadowcore.mercadox.context.filter.JwtAuthFilter;
import hn.shadowcore.mercadox.context.filter.TenantValidatorFilter;
import hn.shadowcore.mercadox.context.security.JwtVerifier;
import hn.shadowcore.mercadox.context.validator.AnonymousTenantValidator;
import hn.shadowcore.mercadox.email.filter.WhatsAppSignatureVerificationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class MercadoXEmailOAuthConfig {

    private final JwtVerifier jwtVerifier;
    private final AnonymousTenantValidator anonymousTenantValidator;
    private final WhatsAppSignatureVerificationFilter signatureVerificationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/public/**").permitAll()
                        // Webhook authenticated by X-Hub-Signature-256, not JWT
                        .requestMatchers("/webhook/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(jwtVerifier), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new TenantValidatorFilter(anonymousTenantValidator), JwtAuthFilter.class)
                .build();
    }

    @Bean
    public FilterRegistrationBean<WhatsAppSignatureVerificationFilter> webhookSignatureFilter() {
        FilterRegistrationBean<WhatsAppSignatureVerificationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(signatureVerificationFilter);
        registration.addUrlPatterns("/webhook");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

}
