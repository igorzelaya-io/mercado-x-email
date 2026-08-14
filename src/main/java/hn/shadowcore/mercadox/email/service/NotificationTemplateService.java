package hn.shadowcore.mercadox.email.service;


import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.model.enums.TemplateChannel;
import hn.shadowcore.mercadox.library.jpa.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;

    public NotificationTemplate save(NotificationTemplate template) {
        return templateRepository.save(template);
    }

    public NotificationTemplate findByNameAndChannel(String name, TemplateChannel templateChannel) {
        return templateRepository.findByOrgIdNameAndChannel(name, templateChannel);
    }

}
