package hn.shadowcore.mercadox.email.service;

import hn.shadowcore.mercadox.email.controller.request.CreateNotificationTemplateRequest;
import hn.shadowcore.mercadox.library.entity.model.auth.Organization;
import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.model.enums.TemplateChannel;
import hn.shadowcore.mercadox.library.jpa.repository.NotificationTemplateRepository;
import hn.shadowcore.mercadox.library.jpa.repository.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;

    public NotificationTemplate save(CreateNotificationTemplateRequest request) {
        Organization organization = organizationRepository
                .findById(UUID.fromString(request.orgId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organization not found: " + request.orgId()));

        NotificationTemplate template = NotificationTemplate.builder()
                .templateKey(request.templateKey())
                .templateChannel(request.channel())
                .languageCode(request.languageCode())
                .subject(request.subject())
                .bodyHtml(request.bodyHtml())
                .whatsappTemplateName(request.whatsappTemplateName())
                .variables(request.variables())
                .active(request.active())
                .systemTemplate(request.systemTemplate())
                .createdAt(Timestamp.from(Instant.now()))
                .build();

        template.setOrganization(organization);
        return templateRepository.save(template);
    }

    public NotificationTemplate findByNameAndChannel(String name, TemplateChannel templateChannel) {
        return templateRepository.findByOrgIdNameAndChannel(name, templateChannel);
    }
}
