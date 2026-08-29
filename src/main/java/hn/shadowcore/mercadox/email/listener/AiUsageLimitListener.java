package hn.shadowcore.mercadox.email.listener;

import hn.shadowcore.mercadox.context.utils.annotations.KafkaIdempotent;
import hn.shadowcore.mercadox.email.service.mailer.MailService;
import hn.shadowcore.mercadox.library.entity.avro.AiUsageLimitExceededEvent;
import hn.shadowcore.mercadox.library.entity.kafka.KafkaTopic;
import hn.shadowcore.mercadox.library.entity.model.auth.Organization;
import hn.shadowcore.mercadox.library.entity.model.auth.User;
import hn.shadowcore.mercadox.library.jpa.repository.OrganizationRepository;
import hn.shadowcore.mercadox.library.jpa.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes ai.usage.limit.exceeded.v1 and emails the org admin — mercado-x-ai fires this
 * once per org per billing cycle when a tenant crosses its plan's monthly Claude quota.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiUsageLimitListener {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @KafkaIdempotent
    @KafkaListener(topics = KafkaTopic.AI_USAGE_LIMIT_EXCEEDED, groupId = "email-service-group")
    public void handleUsageLimitExceeded(ConsumerRecord<String, AiUsageLimitExceededEvent> record) {
        AiUsageLimitExceededEvent event = record.value();
        UUID orgId = UUID.fromString(event.getOrgId());

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + orgId));

        if (organization.getUserAdminId() == null) {
            log.warn("orgId={} has no admin user — cannot deliver usage limit notification", orgId);
            return;
        }

        User admin = userRepository.findById(UUID.fromString(organization.getUserAdminId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Admin user not found: " + organization.getUserAdminId()));

        boolean allowOverage = Boolean.parseBoolean(event.getAllowOverage());
        String subject = "AI plan limit reached — " + organization.getName();
        String body = buildEmailBody(organization.getName(), event, allowOverage);

        mailService.sendToSingleAddress(admin.getEmail(), subject, body);

        log.info("orgId={} plan={} monthlyLimit={} allowOverage={} — usage limit email sent to {}",
                orgId, event.getPlan(), event.getMonthlyLimit(), allowOverage, admin.getEmail());
    }

    private String buildEmailBody(String orgName, AiUsageLimitExceededEvent event, boolean allowOverage) {
        String statusLine = allowOverage
                ? "Your AI assistant is still replying to customers — overage billing applies to messages past your limit."
                : "Your AI assistant has paused replies until your next billing cycle, or until you upgrade your plan.";

        return "<p>Hi " + orgName + " team,</p>"
                + "<p>You've reached your <strong>" + event.getPlan() + "</strong> plan's monthly limit of "
                + event.getMonthlyLimit() + " AI-generated replies.</p>"
                + "<p>" + statusLine + "</p>"
                + "<p>Consider upgrading your plan to keep serving customers without interruption.</p>";
    }
}
