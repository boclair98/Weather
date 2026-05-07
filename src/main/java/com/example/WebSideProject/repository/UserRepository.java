package com.example.WebSideProject.repository;

import com.example.WebSideProject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllBySubscribedTrue();
    Optional<User> findByEmail(String email);
    Optional<User> findByUnsubscribeToken(String unsubscribeToken);
    boolean existsByEmail(String email);
}
