package hn.alturaforge.mercadox.email.listener;

import hn.alturaforge.mercadox.email.service.mailer.MailService;
import hn.alturaforge.mercadox.library.entity.avro.AiUsageLimitExceededEvent;
import hn.alturaforge.mercadox.library.entity.model.auth.Organization;
import hn.alturaforge.mercadox.library.entity.model.auth.User;
import hn.alturaforge.mercadox.library.jpa.repository.OrganizationRepository;
import hn.alturaforge.mercadox.library.jpa.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiUsageLimitListenerTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @Test
    void missingOrganizationFailsClearly() {
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener().handleUsageLimitExceeded(record(event("false"))))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(ORG_ID.toString());

        verifyNoInteractions(userRepository, mailService);
    }

    @Test
    void organizationWithoutAdminIsNotEmailed() {
        Organization organization = Organization.builder().id(ORG_ID).name("Altura Forge").build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));

        listener().handleUsageLimitExceeded(record(event("false")));

        verifyNoInteractions(userRepository, mailService);
    }

    @Test
    void missingAdminFailsClearly() {
        Organization organization = organization();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener().handleUsageLimitExceeded(record(event("false"))))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(ADMIN_ID.toString());

        verify(mailService, never()).sendToSingleAddress(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void hardLimitEmailExplainsThatRepliesArePaused() {
        stubAdmin();

        listener().handleUsageLimitExceeded(record(event("false")));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendToSingleAddress(
                org.mockito.ArgumentMatchers.eq("admin@example.com"),
                org.mockito.ArgumentMatchers.eq("AI plan limit reached — Altura Forge"), body.capture());
        assertThat(body.getValue())
                .contains("STARTER", "500", "paused replies", "upgrade your plan");
    }

    @Test
    void overageEmailExplainsThatRepliesContinue() {
        stubAdmin();

        listener().handleUsageLimitExceeded(record(event("true")));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendToSingleAddress(
                org.mockito.ArgumentMatchers.eq("admin@example.com"),
                org.mockito.ArgumentMatchers.anyString(), body.capture());
        assertThat(body.getValue()).contains("still replying", "overage billing applies");
    }

    private void stubAdmin() {
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization()));
        User admin = User.builder().id(ADMIN_ID).email("admin@example.com").build();
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
    }

    private Organization organization() {
        return Organization.builder()
                .id(ORG_ID)
                .name("Altura Forge")
                .userAdminId(ADMIN_ID.toString())
                .build();
    }

    private AiUsageLimitListener listener() {
        return new AiUsageLimitListener(organizationRepository, userRepository, mailService);
    }

    private ConsumerRecord<String, AiUsageLimitExceededEvent> record(AiUsageLimitExceededEvent event) {
        return new ConsumerRecord<>("ai.usage.limit.exceeded.v1", 0, 0L, "key", event);
    }

    private AiUsageLimitExceededEvent event(String allowOverage) {
        return AiUsageLimitExceededEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType("AI_USAGE_LIMIT_EXCEEDED")
                .setOrgId(ORG_ID.toString())
                .setOccurredAt("2026-09-10T12:00:00Z")
                .setPlan("STARTER")
                .setMonthlyLimit("500")
                .setAllowOverage(allowOverage)
                .build();
    }
}
