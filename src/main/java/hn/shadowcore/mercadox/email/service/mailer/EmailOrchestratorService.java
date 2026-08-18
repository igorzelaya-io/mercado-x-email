package hn.shadowcore.mercadox.email.service.mailer;

import hn.shadowcore.mercadox.email.service.NotificationTemplateService;
import hn.shadowcore.mercadox.email.util.TemplateVariableValidator;
import hn.shadowcore.mercadox.library.entity.avro.EmailRecipient;
import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.model.enums.TemplateChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailOrchestratorService {

    private final NotificationTemplateService notificationTemplateService;
    private final TemplateVariableValidator templateVariableValidator;
    private final TemplateEngine templateEngine;
    private final MailService mailService;

    public void sendToRecipients(String templateName, String subject,
                                 List<EmailRecipient> recipients, Object payload) {
        NotificationTemplate template = findTemplate(templateName);
        templateVariableValidator.validate(payload, template.getVariables());
        for (EmailRecipient recipient : recipients) {
            String content = buildEmailContent(payload, recipient, template);
            mailService.sendToSingleAddress(recipient.getEmail(), subject, content);
        }
    }

    private String buildEmailContent(Object payload, EmailRecipient recipient,
                                     NotificationTemplate template) {
        Context context = new Context();
        context.setVariable("data", payload);
        context.setVariable("recipient", recipient);
        return templateEngine.process(template.getBodyHtml(), context);
    }

    private NotificationTemplate findTemplate(String templateName) {
        return notificationTemplateService.findByNameAndChannel(templateName, TemplateChannel.EMAIL);
    }
}
