package hn.shadowcore.mercadox.email.controller;

import hn.shadowcore.mercadox.email.service.NotificationTemplateService;
import hn.shadowcore.mercadoxlibrary.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadoxlibrary.entity.response.BaseResponseDto;
import hn.shadowcore.mercadoxlibrary.entity.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("permitAll()")
@RequiredArgsConstructor
@RequestMapping("/api/v1/templates")
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<? extends Response<NotificationTemplate>> createTemplate
            (@RequestBody NotificationTemplate notificationTemplate) {
        BaseResponseDto<NotificationTemplate> response = new BaseResponseDto<>();
        NotificationTemplate template = notificationTemplateService.save(notificationTemplate);
        return response.buildResponseEntity(HttpStatus.OK, "Template was created successfully.", template);
    }

}
