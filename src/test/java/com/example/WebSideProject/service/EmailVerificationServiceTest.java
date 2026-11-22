package com.example.WebSideProject.service;

import com.example.WebSideProject.entity.EmailVerificationChallenge;
import com.example.WebSideProject.repository.EmailVerificationChallengeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.lang.reflect.Field;
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
        ArgumentCaptor<EmailVerificationChallenge> challengeCaptor = ArgumentCaptor.forClass(EmailVerificationChallenge.class);
        verify(repository).deleteAllForOwner("owner-123");
        verify(repository).save(challengeCaptor.capture());
        verify(mailService).sendEmailVerificationMail(eq("user@example.com"), codeCaptor.capture());

        assertThat(codeCaptor.getValue()).matches("\\d{6}");
        assertThat(challengeCaptor.getValue().getTokenHash())
                .isEqualTo(scopedHash("owner-123", "user@example.com", codeCaptor.getValue()))
                .isNotEqualTo(hash(codeCaptor.getValue()));
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.message()).contains("6자리");
    }

    @Test
    void requestIsThrottledForThirtySecondsPerOwner() throws Exception {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        MailService mailService = mock(MailService.class);
        EmailVerificationService service = new EmailVerificationService(repository, mailService);
        EmailVerificationChallenge recent = EmailVerificationChallenge.issue(
                "owner-123",
                "first@example.com",
                hash("042731"),
                LocalDateTime.now().plusMinutes(15)
        );
        Field createdAt = EmailVerificationChallenge.class.getDeclaredField("createdAt");
        createdAt.setAccessible(true);
        createdAt.set(recent, LocalDateTime.now().minusSeconds(5));
        when(repository.findFirstByOwnerIdOrderByCreatedAtDesc("owner-123"))
                .thenReturn(Optional.of(recent));

        assertThatThrownBy(() -> service.requestVerification("owner-123", "second@example.com"))
                .isInstanceOf(EmailVerificationCooldownException.class)
                .hasMessageContaining("초 후")
                .hasMessageContaining("너무 자주");
        verify(repository, org.mockito.Mockito.never()).deleteAllForOwner(any());
        verify(mailService, org.mockito.Mockito.never()).sendEmailVerificationMail(any(), any());
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

    @Test
    void theSameCodeCanBeConfirmedAndConsumedForDifferentOwnersAndEmails() {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        EmailVerificationService service = new EmailVerificationService(repository, mock(MailService.class));
        String code = "042731";
        EmailVerificationChallenge first = scopedChallenge("first-owner", "first@example.com", code);
        EmailVerificationChallenge second = scopedChallenge("second-owner", "second@example.com", code);
        when(repository.findFirstByOwnerIdAndEmailOrderByCreatedAtDesc("first-owner", "first@example.com"))
                .thenReturn(Optional.of(first));
        when(repository.findFirstByOwnerIdAndEmailOrderByCreatedAtDesc("second-owner", "second@example.com"))
                .thenReturn(Optional.of(second));

        service.confirmCode("first-owner", "first@example.com", code);
        service.confirmCode("second-owner", "second@example.com", code);

        assertThat(first.getTokenHash()).isNotEqualTo(second.getTokenHash());
        assertThat(service.consumeVerifiedEmail("first-owner", code, "first@example.com"))
                .isEqualTo("first@example.com");
        assertThat(service.consumeVerifiedEmail("second-owner", code, "second@example.com"))
                .isEqualTo("second@example.com");
        assertThat(first.isConsumed()).isTrue();
        assertThat(second.isConsumed()).isTrue();
        verify(repository, org.mockito.Mockito.never()).findByTokenHash(any());
    }

    @Test
    void changingTheEmailForTheSameOwnerDoesNotReuseACodeHash() {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        EmailVerificationService service = new EmailVerificationService(repository, mock(MailService.class));
        String code = "042731";
        EmailVerificationChallenge first = scopedChallenge("owner", "first@example.com", code);
        EmailVerificationChallenge second = scopedChallenge("owner", "second@example.com", code);
        when(repository.findFirstByOwnerIdAndEmailOrderByCreatedAtDesc("owner", "first@example.com"))
                .thenReturn(Optional.of(first));
        when(repository.findFirstByOwnerIdAndEmailOrderByCreatedAtDesc("owner", "second@example.com"))
                .thenReturn(Optional.of(second));

        service.confirmCode("owner", "first@example.com", code);
        service.confirmCode("owner", "second@example.com", code);

        assertThat(first.isVerified()).isTrue();
        assertThat(second.isVerified()).isTrue();
        assertThat(first.getTokenHash()).isNotEqualTo(second.getTokenHash());
    }

    @Test
    void anotherAccountCannotConsumeAVerifiedCodeEvenIfItKnowsTheCode() {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        EmailVerificationService service = new EmailVerificationService(repository, mock(MailService.class));
        EmailVerificationChallenge challenge = scopedChallenge("original-owner", "user@example.com", "042731");
        challenge.markVerified(LocalDateTime.now());
        when(repository.findFirstByOwnerIdAndEmailOrderByCreatedAtDesc("original-owner", "user@example.com"))
                .thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> service.consumeVerifiedEmail("other-owner", "042731", "user@example.com"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("현재 계정");
        assertThat(challenge.isConsumed()).isFalse();
    }

    @Test
    void changingTheRecipientDoesNotConsumeTheOriginalChallenge() {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        EmailVerificationService service = new EmailVerificationService(repository, mock(MailService.class));
        EmailVerificationChallenge challenge = scopedChallenge("owner", "first@example.com", "042731");
        challenge.markVerified(LocalDateTime.now());
        when(repository.findFirstByOwnerIdAndEmailOrderByCreatedAtDesc("owner", "first@example.com"))
                .thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> service.consumeVerifiedEmail("owner", "042731", "second@example.com"))
                .isInstanceOf(SecurityException.class);
        assertThat(challenge.isConsumed()).isFalse();
    }

    @Test
    void anAlreadyIssuedUnscopedCodeStillWorksUntilItExpires() {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        EmailVerificationService service = new EmailVerificationService(repository, mock(MailService.class));
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(
                "owner", "user@example.com", hash("042731"), LocalDateTime.now().plusMinutes(15)
        );
        when(repository.findFirstByOwnerIdAndEmailOrderByCreatedAtDesc("owner", "user@example.com"))
                .thenReturn(Optional.of(challenge));

        service.confirmCode("owner", "user@example.com", "042731");

        assertThat(service.consumeVerifiedEmail("owner", "042731", "user@example.com"))
                .isEqualTo("user@example.com");
        assertThat(challenge.isConsumed()).isTrue();
        assertThatThrownBy(() -> service.consumeVerifiedEmail("owner", "042731", "user@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 구독에 사용");
    }

    @Test
    void oldLongLinkTokensStillRequireTheOriginalOwner() {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        EmailVerificationService service = new EmailVerificationService(repository, mock(MailService.class));
        String token = "legacy-link-token-with-at-least-thirty-two-characters";
        EmailVerificationChallenge challenge = EmailVerificationChallenge.issue(
                "original-owner", "user@example.com", hash(token), LocalDateTime.now().plusMinutes(15)
        );
        when(repository.findByTokenHash(hash(token))).thenReturn(Optional.of(challenge));

        service.confirm(token);

        assertThatThrownBy(() -> service.consumeVerifiedEmail("other-owner", token, "user@example.com"))
                .isInstanceOf(SecurityException.class);
        assertThat(challenge.isConsumed()).isFalse();
        assertThat(service.consumeVerifiedEmail("original-owner", token, "user@example.com"))
                .isEqualTo("user@example.com");
    }

    @Test
    void deprecatedLinkConfirmationCannotApproveASixDigitCode() {
        EmailVerificationChallengeRepository repository = mock(EmailVerificationChallengeRepository.class);
        EmailVerificationService service = new EmailVerificationService(repository, mock(MailService.class));

        assertThatThrownBy(() -> service.confirm("042731"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("입력 화면");
        verify(repository, org.mockito.Mockito.never()).findByTokenHash(any());
    }

    private EmailVerificationChallenge scopedChallenge(String owner, String email, String code) {
        return EmailVerificationChallenge.issue(owner, email, scopedHash(owner, email, code), LocalDateTime.now().plusMinutes(15));
    }

    private String scopedHash(String owner, String email, String code) {
        return hash("email-code-v2:" + owner.length() + ":" + owner + ":" + email + ":" + code);
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
