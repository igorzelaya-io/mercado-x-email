package hn.shadowcore.mercadox.email.service.mailer;

import hn.shadowcore.mercadox.email.util.TemplateVariableValidator;
import hn.shadowcore.mercadox.email.service.NotificationTemplateService;
import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.model.enums.TemplateChannel;
import hn.shadowcore.mercadox.library.entity.response.dto.EmailEventDto;
import hn.shadowcore.mercadox.library.entity.response.dto.EmailRecipientDto;
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

    public <T> void sendToMultipleRecipients(EmailEventDto<T> eventDto, String templateName) {
        NotificationTemplate template = findTemplate(templateName);
        validateTemplateArgs(eventDto.getPayload(), template.getVariables());
        for(EmailRecipientDto recipient : eventDto.getRecipients()) {
            String emailContent = buildEmailContent(eventDto.getPayload(), recipient, template);
            mailService.sendToSingleAddress(recipient.email(), eventDto.getEventSubject(), emailContent);
        }
    }
    private <T> String buildEmailContent(T payload, EmailRecipientDto recipient, NotificationTemplate template) {

        Context context = new Context();
        context.setVariable("data", payload);
        context.setVariable("recipient", recipient);

        return templateEngine.process(template.getBodyHtml(), context);
    }

    private <T> void validateTemplateArgs(T payload, List<String> expectedVariables) {
        templateVariableValidator.validate(payload, expectedVariables);
    }

    private NotificationTemplate findTemplate(String templateName) {
        return notificationTemplateService.findByNameAndChannel(templateName, TemplateChannel.EMAIL);
    }

}
