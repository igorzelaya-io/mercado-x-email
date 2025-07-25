package hn.shadowcore.mercadoxemail.service;

import hn.shadowcore.mercadoxlibrary.entity.response.dto.EmailEventDto;
import hn.shadowcore.mercadoxlibrary.entity.response.dto.EmailRecipientDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailOrchestrator {
    private TemplateEngine templateEngine;
    private MailService mailService;

    public <T> void sendToMultipleRecipients(EmailEventDto<T> eventDto, String templateName) {
        for(EmailRecipientDto recipient : eventDto.getRecipients()) {
            String emailContent = buildEmailContent(eventDto.getPayload(), recipient, templateName);
            mailService.sendToSingleAddress(recipient.email(), eventDto.getEventSubject(), emailContent);
        }
    }
    private <T> String buildEmailContent(T payload, EmailRecipientDto recipient, String templateName) {

        Context context = new Context();
        context.setVariable("data", payload);
        context.setVariable("recipient", recipient);

        return templateEngine.process(templateName, context);
    }

}
