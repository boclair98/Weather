package com.example.WebSideProject.repository;

import com.example.WebSideProject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllBySubscribedTrue();
    Optional<User> findByEmail(String email);
    Optional<User> findByCodersUserId(String codersUserId);
    Optional<User> findFirstByOwnerIdOrderByIdAsc(String ownerId);
    Optional<User> findByUnsubscribeToken(String unsubscribeToken);
    boolean existsByEmail(String email);

    @Modifying
    @Query(value = "DELETE FROM weather_mail_histories WHERE user_email = :email", nativeQuery = true)
    int deleteMailHistoriesByEmail(@Param("email") String email);
}
