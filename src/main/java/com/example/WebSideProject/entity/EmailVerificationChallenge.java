package com.example.WebSideProject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_verification_challenges",
        indexes = {
                @Index(name = "idx_email_verification_owner", columnList = "ownerId"),
                @Index(name = "idx_email_verification_expires", columnList = "expiresAt")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String ownerId;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    private LocalDateTime consumedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private EmailVerificationChallenge(
            String ownerId,
            String email,
            String tokenHash,
            LocalDateTime expiresAt
    ) {
        this.ownerId = ownerId;
        this.email = email;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static EmailVerificationChallenge issue(
            String ownerId,
            String email,
            String tokenHash,
            LocalDateTime expiresAt
    ) {
        return new EmailVerificationChallenge(ownerId, email, tokenHash, expiresAt);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt == null || now == null || !expiresAt.isAfter(now);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void markVerified(LocalDateTime now) {
        if (!isExpired(now) && !isConsumed()) {
            verifiedAt = now;
        }
    }

    public void markConsumed(LocalDateTime now) {
        if (!isVerified() || isExpired(now) || isConsumed()) {
            throw new IllegalStateException("이메일 인증이 만료되었거나 이미 사용되었습니다.");
        }
        consumedAt = now;
    }
}
