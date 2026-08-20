package com.example.WebSideProject.controller;

import com.example.WebSideProject.dto.UserDto;
import com.example.WebSideProject.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @Valid @RequestBody UserDto.RegisterRequest request,
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        List<UserDto.Response> responses = userService.registerAll(request, codersUserId);
        if (responses.size() == 1) {
            return ResponseEntity.created(URI.create("/api/users/me")).body(responses.get(0));
        }
        UserDto.BatchResponse response = UserDto.BatchResponse.builder()
                .successCount(responses.size())
                .recipients(responses.stream().map(UserDto.Response::getEmail).toList())
                .message(responses.size() + "개의 이메일 구독이 완료되었습니다! 각 메일함을 확인해주세요.")
                .build();
        return ResponseEntity.created(URI.create("/api/users/me")).body(response);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<UserDto.BatchResponse> unsubscribeByEmail(
            @Valid @RequestBody UserDto.UnsubscribeRequest request,
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        List<UserDto.Response> responses = userService.unsubscribeAll(request, codersUserId);
        UserDto.BatchResponse response = UserDto.BatchResponse.builder()
                .successCount(responses.size())
                .recipients(responses.stream().map(UserDto.Response::getEmail).toList())
                .message(responses.size() + "개의 이메일 구독이 취소되었습니다.")
                .build();
        return ResponseEntity.ok(response);
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

    @PatchMapping("/me/smart-alerts")
    public ResponseEntity<UserDto.Response> updateCurrentSmartAlerts(
            @RequestBody UserDto.UpdateSmartAlertRequest request,
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        return ResponseEntity.ok(userService.updateSmartAlerts(request, codersUserId));
    }

    @DeleteMapping("/me/subscription")
    public ResponseEntity<UserDto.Response> unsubscribeCurrent(
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        return ResponseEntity.ok(userService.unsubscribeCurrent(codersUserId));
    }

    @DeleteMapping("/me/data")
    public ResponseEntity<Void> deleteCurrentData(
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        userService.deleteCurrentData(codersUserId);
        return ResponseEntity.noContent().build();
    }
}
