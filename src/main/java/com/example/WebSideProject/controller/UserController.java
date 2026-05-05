package com.example.WebSideProject.controller;

import com.example.WebSideProject.dto.UserDto;
import com.example.WebSideProject.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/subscribe")
    public ResponseEntity<UserDto.Response> subscribe(@Valid @RequestBody UserDto.RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PatchMapping("/unsubscribe")
    public ResponseEntity<UserDto.Response> unsubscribe(@RequestParam String email) {
        return ResponseEntity.ok(userService.unsubscribe(email));
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<UserDto.Response> unsubscribeFromMail(@RequestParam String email) {
        return ResponseEntity.ok(userService.unsubscribe(email));
    }

    @PatchMapping("/resubscribe")
    public ResponseEntity<UserDto.Response> resubscribe(@RequestParam String email) {
        return ResponseEntity.ok(userService.resubscribe(email));
    }

    @PatchMapping("/location")
    public ResponseEntity<UserDto.Response> updateLocation(@Valid @RequestBody UserDto.UpdateLocationRequest request) {
        return ResponseEntity.ok(userService.updateLocation(request));
    }

    @PatchMapping("/style-preference")
    public ResponseEntity<UserDto.Response> updateStylePreference(
            @Valid @RequestBody UserDto.UpdateStylePreferenceRequest request
    ) {
        return ResponseEntity.ok(userService.updateStylePreference(request));
    }
}
