package hn.shadowcore.mercadox.email.controller.request;

import hn.shadowcore.mercadox.library.entity.model.enums.TemplateChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateNotificationTemplateRequest(

        @NotBlank
        String templateKey,

        @NotNull
        TemplateChannel channel,

        @NotBlank
        String languageCode,

        @NotBlank
        String orgId,

        String subject,

        String bodyHtml,

        String whatsappTemplateName,

        List<String> variables,

        Boolean active,

        Boolean systemTemplate
) {}
