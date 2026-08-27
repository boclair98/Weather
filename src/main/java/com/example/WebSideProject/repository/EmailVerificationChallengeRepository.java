package com.example.WebSideProject.repository;

import com.example.WebSideProject.entity.EmailVerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationChallengeRepository extends JpaRepository<EmailVerificationChallenge, Long> {

    Optional<EmailVerificationChallenge> findByTokenHash(String tokenHash);

    Optional<EmailVerificationChallenge> findFirstByOwnerIdAndEmailOrderByCreatedAtDesc(
            String ownerId,
            String email
    );

    @Modifying
    @Query("delete from EmailVerificationChallenge c where c.ownerId = :ownerId")
    int deleteAllForOwner(@Param("ownerId") String ownerId);

    @Modifying
    @Query("delete from EmailVerificationChallenge c where c.ownerId = :ownerId and (c.consumedAt is not null or c.expiresAt <= :now)")
    int deleteObsoleteForOwner(@Param("ownerId") String ownerId, @Param("now") LocalDateTime now);
}
