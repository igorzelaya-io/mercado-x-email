package hn.shadowcore.mercadox.email.controller;

import hn.shadowcore.mercadox.email.controller.request.CreateNotificationTemplateRequest;
import hn.shadowcore.mercadox.email.service.NotificationTemplateService;
import hn.shadowcore.mercadox.library.entity.model.core.NotificationTemplate;
import hn.shadowcore.mercadox.library.entity.response.BaseResponseDto;
import hn.shadowcore.mercadox.library.entity.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/templates")
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<? extends Response<NotificationTemplate>> createTemplate(
            @Valid @RequestBody CreateNotificationTemplateRequest request) {
        BaseResponseDto<NotificationTemplate> response = new BaseResponseDto<>();
        NotificationTemplate template = notificationTemplateService.save(request);
        return response.buildResponseEntity(HttpStatus.CREATED, "Template was created successfully.", template);
    }
}
