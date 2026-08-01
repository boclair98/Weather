package com.example.WebSideProject.controller;

import com.example.WebSideProject.dto.UserDto;
import com.example.WebSideProject.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/subscribe")
    public ResponseEntity<UserDto.Response> subscribe(
            @Valid @RequestBody UserDto.RegisterRequest request,
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        UserDto.Response response = userService.register(request, codersUserId);
        return ResponseEntity.created(URI.create("/api/users/me")).body(response);
    }

    @PatchMapping("/unsubscribe")
    public ResponseEntity<UserDto.Response> unsubscribe(
            @RequestParam String email,
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        return ResponseEntity.ok(userService.unsubscribe(email, codersUserId));
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<UserDto.Response> unsubscribeFromMail(
            @RequestParam String token
    ) {
        return ResponseEntity.ok(userService.unsubscribeByToken(token));
    }

    @PatchMapping("/resubscribe")
    public ResponseEntity<UserDto.Response> resubscribe(
            @RequestParam String email,
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        return ResponseEntity.ok(userService.resubscribe(email, codersUserId));
    }

    @PatchMapping("/location")
    public ResponseEntity<UserDto.Response> updateLocation(
            @Valid @RequestBody UserDto.UpdateLocationRequest request,
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        return ResponseEntity.ok(userService.updateLocation(request, codersUserId));
    }

    @PatchMapping("/style-preference")
    public ResponseEntity<UserDto.Response> updateStylePreference(
            @Valid @RequestBody UserDto.UpdateStylePreferenceRequest request,
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        return ResponseEntity.ok(userService.updateStylePreference(request, codersUserId));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto.Response> getCurrentSubscription(
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        return userService.findCurrentSubscription(codersUserId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/me/notifications")
    public ResponseEntity<UserDto.Response> updateCurrentNotifications(
            @RequestBody UserDto.UpdateNotificationRequest request,
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        return ResponseEntity.ok(userService.updateNotificationTimes(request, codersUserId));
    }

    @DeleteMapping("/me/subscription")
    public ResponseEntity<UserDto.Response> unsubscribeCurrent(
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        return ResponseEntity.ok(userService.unsubscribeCurrent(codersUserId));
    }
}
