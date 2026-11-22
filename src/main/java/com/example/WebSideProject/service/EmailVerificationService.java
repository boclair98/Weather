package com.example.WebSideProject.service;

import com.example.WebSideProject.entity.EmailVerificationChallenge;
import com.example.WebSideProject.repository.EmailVerificationChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int TOKEN_TTL_MINUTES = 15;
    private static final int CODE_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationChallengeRepository challengeRepository;
    private final MailService mailService;

    @Transactional
    public VerificationResponse requestVerification(String ownerId, String email) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedOwnerId = resolveOwnerId(ownerId, normalizedEmail);
        LocalDateTime now = LocalDateTime.now();
        challengeRepository.findFirstByOwnerIdOrderByCreatedAtDesc(normalizedOwnerId)
                .ifPresent(latest -> enforceResendCooldown(latest, now));
        // A newly requested code always invalidates the previous one. This keeps
        // multiple tabs or repeated requests from leaving several valid codes.
        challengeRepository.deleteAllForOwner(normalizedOwnerId);

        String verificationCode = generateVerificationCode();
        LocalDateTime expiresAt = now.plusMinutes(TOKEN_TTL_MINUTES);
        challengeRepository.save(EmailVerificationChallenge.issue(
                normalizedOwnerId,
                normalizedEmail,
                hashCode(normalizedOwnerId, normalizedEmail, verificationCode),
                expiresAt
        ));

        mailService.sendEmailVerificationMail(normalizedEmail, verificationCode);

        return new VerificationResponse(
                normalizedEmail,
                expiresAt,
                "인증번호를 보냈습니다. 메일에서 6자리 번호를 확인해 15분 안에 입력해주세요."
        );
    }

    private void enforceResendCooldown(EmailVerificationChallenge latest, LocalDateTime now) {
        LocalDateTime createdAt = latest.getCreatedAt();
        if (createdAt == null) {
            return;
        }
        long elapsedSeconds = Duration.between(createdAt, now).getSeconds();
        if (elapsedSeconds < RESEND_COOLDOWN_SECONDS) {
            long retryAfter = Math.max(1, RESEND_COOLDOWN_SECONDS - Math.max(0, elapsedSeconds));
            throw new EmailVerificationCooldownException(
                    "인증번호를 너무 자주 요청했어요. " + retryAfter + "초 후 다시 시도해주세요.",
                    retryAfter
            );
        }
    }

    /**
     * Compares the number entered in the browser with the latest challenge for
     * that signed-in owner and email. A successful comparison marks the
     * challenge as verified; the same code is then consumed by /subscribe.
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public VerificationResponse confirmCode(String ownerId, String email, String rawCode) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedOwnerId = resolveOwnerId(ownerId, normalizedEmail);
        String code = normalizeCode(rawCode);
        EmailVerificationChallenge challenge = challengeRepository
                .findFirstByOwnerIdAndEmailOrderByCreatedAtDesc(normalizedOwnerId, normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "인증번호 요청 내역이 없습니다. 먼저 인증번호를 받아주세요."
                ));

        LocalDateTime now = LocalDateTime.now();
        if (challenge.isExpired(now)) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 새 인증번호를 요청해주세요.");
        }
        if (challenge.isConsumed()) {
            throw new IllegalArgumentException("이미 구독에 사용된 인증번호입니다. 새 인증번호를 요청해주세요.");
        }
        if (challenge.isVerified()) {
            if (!matchesCode(code, challenge)) {
                throw new IllegalArgumentException("인증번호가 일치하지 않습니다. 다시 확인해주세요.");
            }
            return new VerificationResponse(
                    normalizedEmail,
                    challenge.getExpiresAt(),
                    "이메일 인증이 이미 완료됐습니다. 이제 구독을 시작해주세요."
            );
        }
        if (challenge.getFailedAttempts() >= MAX_CODE_ATTEMPTS) {
            throw new IllegalArgumentException("인증번호 입력 횟수를 초과했습니다. 새 인증번호를 요청해주세요.");
        }
        if (!matchesCode(code, challenge)) {
            challenge.recordFailedAttempt();
            int remaining = Math.max(0, MAX_CODE_ATTEMPTS - challenge.getFailedAttempts());
            if (remaining == 0) {
                throw new IllegalArgumentException(
                        "인증번호가 일치하지 않습니다. 입력 가능 횟수를 모두 사용했어요. 새 인증번호를 요청해주세요."
                );
            }
            throw new IllegalArgumentException(
                    "인증번호가 일치하지 않습니다. 다시 확인해주세요. 남은 시도 " + remaining + "회"
            );
        }

        challenge.markVerified(now);
        return new VerificationResponse(
                normalizedEmail,
                challenge.getExpiresAt(),
                "이메일 인증이 완료됐습니다. 이제 구독을 시작해주세요."
        );
    }

    @Transactional
    public void confirm(String rawToken) {
        EmailVerificationChallenge challenge = findChallenge(rawToken);
        LocalDateTime now = LocalDateTime.now();
        if (challenge.isExpired(now)) {
            throw new IllegalArgumentException("이메일 인증 링크가 만료되었습니다. 다시 인증 메일을 요청해주세요.");
        }
        if (challenge.isConsumed()) {
            throw new IllegalArgumentException("이미 사용된 이메일 인증 링크입니다.");
        }
        challenge.markVerified(now);
    }

    @Transactional
    public String consumeVerifiedEmail(String ownerId, String rawToken) {
        return consumeVerifiedEmail(ownerId, rawToken, null);
    }

    @Transactional
    public String consumeVerifiedEmail(String ownerId, String rawToken, String expectedEmail) {
        EmailVerificationChallenge challenge = isVerificationCode(rawToken)
                ? findCodeChallenge(ownerId, rawToken.trim(), expectedEmail)
                : findChallenge(rawToken);
        String normalizedOwnerId = resolveOwnerId(ownerId, challenge.getEmail());
        LocalDateTime now = LocalDateTime.now();
        if (!normalizedOwnerId.equals(challenge.getOwnerId())) {
            throw new SecurityException("현재 로그인 계정으로 요청한 이메일 인증만 사용할 수 있습니다.");
        }
        if (challenge.isExpired(now)) {
            throw new IllegalArgumentException("이메일 인증이 만료되었습니다. 인증 메일을 다시 요청해주세요.");
        }
        if (challenge.isConsumed()) {
            throw new IllegalArgumentException("이미 구독에 사용된 인증번호입니다. 새 인증번호를 요청해주세요.");
        }
        if (!challenge.isVerified()) {
            throw new SecurityException("이메일 인증번호를 먼저 확인해주세요.");
        }
        if (expectedEmail != null && !normalizeEmail(expectedEmail).equals(challenge.getEmail())) {
            throw new SecurityException("인증한 이메일과 구독 요청 이메일이 일치하지 않습니다.");
        }
        challenge.markConsumed(now);
        return challenge.getEmail();
    }

    private EmailVerificationChallenge findCodeChallenge(String ownerId, String code, String expectedEmail) {
        EmailVerificationChallenge challenge;
        if (expectedEmail != null) {
            String email = normalizeEmail(expectedEmail);
            String normalizedOwnerId = resolveOwnerId(ownerId, email);
            challenge = challengeRepository
                    .findFirstByOwnerIdAndEmailOrderByCreatedAtDesc(normalizedOwnerId, email)
                    .orElseThrow(() -> new SecurityException(
                            "현재 계정과 이메일로 요청한 인증번호만 사용할 수 있습니다."
                    ));
        } else if (ownerId != null && !ownerId.isBlank()) {
            // Older callers may only provide the signed-in owner. Each owner
            // has at most one current challenge because requests replace it.
            challenge = challengeRepository.findFirstByOwnerIdOrderByCreatedAtDesc(ownerId.trim())
                    .orElseThrow(() -> new SecurityException("현재 계정으로 요청한 인증번호만 사용할 수 있습니다."));
        } else {
            throw new IllegalArgumentException("인증한 이메일 주소를 함께 입력해주세요.");
        }
        if (!matchesCode(code, challenge)) {
            throw new SecurityException("유효하지 않은 이메일 인증번호입니다.");
        }
        return challenge;
    }

    private EmailVerificationChallenge findChallenge(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            throw new IllegalArgumentException("유효한 이메일 인증 토큰이 필요합니다.");
        }
        if (isVerificationCode(rawToken)) {
            // Code verification must use the owner/email-scoped POST flow;
            // the deprecated link endpoint must not approve a guessed code.
            throw new IllegalArgumentException("이메일 인증번호는 입력 화면에서 확인해주세요.");
        }
        return challengeRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 이메일 인증번호입니다."));
    }

    private String resolveOwnerId(String ownerId, String fallbackEmail) {
        if (ownerId != null && !ownerId.isBlank()) {
            return ownerId.trim();
        }
        return hash(normalizeEmail(fallbackEmail));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("본인 이메일을 입력해주세요.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
        return normalized;
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("이메일 인증값을 생성하지 못했습니다.", e);
        }
    }

    private String generateVerificationCode() {
        return String.format(Locale.ROOT, "%0" + CODE_LENGTH + "d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String normalizeCode(String rawCode) {
        if (rawCode == null || !rawCode.trim().matches("\\d{" + CODE_LENGTH + "}")) {
            throw new IllegalArgumentException("이메일 인증번호 6자리를 입력해주세요.");
        }
        return rawCode.trim();
    }

    private boolean isVerificationCode(String credential) {
        return credential != null && credential.trim().matches("\\d{" + CODE_LENGTH + "}");
    }

    private String hashCode(String ownerId, String email, String code) {
        // Length-prefix the owner so unusual identity strings cannot collide
        // with the separator. No code is globally unique across subscribers.
        return hash("email-code-v2:" + ownerId.length() + ":" + ownerId + ":" + email + ":" + code);
    }

    private boolean matchesCode(String code, EmailVerificationChallenge challenge) {
        String scopedHash = hashCode(challenge.getOwnerId(), challenge.getEmail(), code);
        if (constantTimeEquals(scopedHash, challenge.getTokenHash())) return true;
        // Keep already-issued codes working during their 15-minute lifetime.
        return matchesHash(code, challenge.getTokenHash());
    }

    private boolean matchesHash(String rawCode, String storedHash) {
        return constantTimeEquals(hash(rawCode), storedHash);
    }

    private boolean constantTimeEquals(String expectedHash, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    public record VerificationResponse(String email, LocalDateTime expiresAt, String message) {
    }
}
