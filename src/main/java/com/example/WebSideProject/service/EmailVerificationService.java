package com.example.WebSideProject.service;

import com.example.WebSideProject.entity.EmailVerificationChallenge;
import com.example.WebSideProject.repository.EmailVerificationChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int TOKEN_TTL_MINUTES = 15;

    private final EmailVerificationChallengeRepository challengeRepository;
    private final MailService mailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Transactional
    public VerificationResponse requestVerification(String ownerId, String email) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedOwnerId = resolveOwnerId(ownerId, normalizedEmail);
        LocalDateTime now = LocalDateTime.now();
        challengeRepository.deleteObsoleteForOwner(normalizedOwnerId, now);

        String rawToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = now.plusMinutes(TOKEN_TTL_MINUTES);
        challengeRepository.save(EmailVerificationChallenge.issue(
                normalizedOwnerId,
                normalizedEmail,
                hash(rawToken),
                expiresAt
        ));

        String verificationUrl = UriComponentsBuilder
                .fromUriString(appBaseUrl.replaceAll("/+$", ""))
                .path("/api/users/email-verification/confirm")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
        mailService.sendEmailVerificationMail(normalizedEmail, verificationUrl);

        return new VerificationResponse(
                normalizedEmail,
                expiresAt,
                "인증 메일을 보냈습니다. 15분 안에 메일의 인증 버튼을 눌러주세요."
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
        EmailVerificationChallenge challenge = findChallenge(rawToken);
        String normalizedOwnerId = resolveOwnerId(ownerId, challenge.getEmail());
        LocalDateTime now = LocalDateTime.now();
        if (!normalizedOwnerId.equals(challenge.getOwnerId())) {
            throw new SecurityException("현재 로그인 계정으로 요청한 이메일 인증만 사용할 수 있습니다.");
        }
        if (challenge.isExpired(now)) {
            throw new IllegalArgumentException("이메일 인증이 만료되었습니다. 인증 메일을 다시 요청해주세요.");
        }
        if (!challenge.isVerified()) {
            throw new SecurityException("이메일 인증 링크를 먼저 눌러주세요.");
        }
        if (expectedEmail != null && !normalizeEmail(expectedEmail).equals(challenge.getEmail())) {
            throw new SecurityException("인증한 이메일과 구독 요청 이메일이 일치하지 않습니다.");
        }
        challenge.markConsumed(now);
        return challenge.getEmail();
    }

    private EmailVerificationChallenge findChallenge(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            throw new IllegalArgumentException("유효한 이메일 인증 토큰이 필요합니다.");
        }
        return challengeRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 이메일 인증 링크입니다."));
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
        String normalized = email.trim().toLowerCase();
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
            throw new IllegalStateException("이메일 인증 토큰을 생성하지 못했습니다.", e);
        }
    }

    public record VerificationResponse(String email, LocalDateTime expiresAt, String message) {
    }
}
