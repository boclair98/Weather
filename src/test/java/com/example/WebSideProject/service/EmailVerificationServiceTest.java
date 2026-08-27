package com.example.WebSideProject.service;

import com.example.WebSideProject.entity.EmailVerificationChallenge;
import com.example.WebSideProject.repository.EmailVerificationChallengeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

    @Test
    void requestSendsSixDigitCodeAndInvalidatesPreviousChallenges() {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        MailService mailService = mock(MailService.class);
        EmailVerificationService service = new EmailVerificationService(repository, mailService);

        EmailVerificationService.VerificationResponse response = service
                .requestVerification("owner-123", " User@Example.com ");

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).deleteAllForOwner("owner-123");
        verify(repository).save(any(EmailVerificationChallenge.class));
        verify(mailService).sendEmailVerificationMail(eq("user@example.com"), codeCaptor.capture());

        assertThat(codeCaptor.getValue()).matches("\\d{6}");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.message()).contains("6자리");
    }

    @Test
    void matchingCodeApprovesTheChallenge() {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        EmailVerificationService service = new EmailVerificationService(repository, mock(MailService.class));
        String code = "042731";
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(
                "owner-123",
                "user@example.com",
                hash(code),
                LocalDateTime.now().plusMinutes(15)
        );
        when(repository.findFirstByOwnerIdAndEmailOrderByCreatedAtDesc("owner-123", "user@example.com"))
                .thenReturn(Optional.of(challenge));

        EmailVerificationService.VerificationResponse response = service
                .confirmCode("owner-123", "user@example.com", " 042731 ");

        assertThat(challenge.isVerified()).isTrue();
        assertThat(response.message()).contains("완료");
    }

    @Test
    void wrongCodeIsRejectedAndAfterFiveAttemptsChallengeIsLocked() {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        EmailVerificationService service = new EmailVerificationService(repository, mock(MailService.class));
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(
                "owner-123",
                "user@example.com",
                hash("042731"),
                LocalDateTime.now().plusMinutes(15)
        );
        when(repository.findFirstByOwnerIdAndEmailOrderByCreatedAtDesc("owner-123", "user@example.com"))
                .thenReturn(Optional.of(challenge));

        for (int attempt = 1; attempt <= 4; attempt++) {
            int remaining = 5 - attempt;
            assertThatThrownBy(() -> service.confirmCode("owner-123", "user@example.com", "000000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("일치하지 않습니다")
                    .hasMessageContaining("" + remaining);
        }
        assertThatThrownBy(() -> service.confirmCode("owner-123", "user@example.com", "000000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("모두 사용");
        assertThat(challenge.getFailedAttempts()).isEqualTo(5);
        assertThat(challenge.isVerified()).isFalse();
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
