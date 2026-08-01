package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.UserDto;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final WeatherService weatherService;
    private final MailService mailService;

    @Value("${coders.identity.required:false}")
    private boolean codersIdentityRequired;

    @Transactional
    public UserDto.Response register(UserDto.RegisterRequest request, String codersUserId) {
        requireCodersIdentity(codersUserId);
        String email = normalizeEmail(request.getEmail());
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.updateName(request.getName());
            user.claimCodersIdentity(normalizeCodersUserId(codersUserId));
            user.updateLocation(
                    request.getLocationName(),
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getNx(),
                    request.getNy()
            );
            user.updateStylePreference(request.getAgeGroup(), request.getGender());
            user.updateNotificationTimes(
                    request.isMorningEnabled(),
                    request.isAfternoonEnabled(),
                    request.isEveningEnabled()
            );
            user.subscribe();
            sendWelcomeWeatherMail(user);
            return toResponse(user, "구독 정보가 새롭게 업데이트되었습니다.");
        }

        boolean hasNotificationTime = request.isMorningEnabled()
                || request.isAfternoonEnabled()
                || request.isEveningEnabled();
        boolean morningEnabled = request.isMorningEnabled() || !hasNotificationTime;

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .codersUserId(normalizeCodersUserId(codersUserId))
                .locationName(request.getLocationName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .nx(request.getNx())
                .ny(request.getNy())
                .ageGroup(request.getAgeGroup())
                .gender(request.getGender())
                .morningEnabled(morningEnabled)
                .afternoonEnabled(request.isAfternoonEnabled())
                .eveningEnabled(request.isEveningEnabled())
                .build();

        User saved = userRepository.save(user);
        log.info("신규 구독자 등록: {}", saved.getEmail());
        sendWelcomeWeatherMail(saved);

        return UserDto.Response.builder()
                .id(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .subscribed(saved.isSubscribed())
                .locationName(saved.getLocationName())
                .ageGroup(saved.getAgeGroup())
                .gender(saved.getGender())
                .morningEnabled(saved.isMorningEnabled())
                .afternoonEnabled(saved.isAfternoonEnabled())
                .eveningEnabled(saved.isEveningEnabled())
                .message("구독이 완료되었습니다! 선택한 시간에 날씨를 보내드릴게요 🌤️")
                .build();
    }

    private void sendWelcomeWeatherMail(User user) {
        try {
            WeatherDto weather = weatherService.getWeather(user.getNx(), user.getNy(), WeatherPeriod.MORNING, user.getLocationName());
            mailService.sendWeatherMail(user, weather);
        } catch (Exception e) {
            log.error("구독 직후 날씨 메일 발송 준비 실패: {}", user.getEmail(), e);
        }
    }

    @Transactional
    public UserDto.Response unsubscribe(String email, String codersUserId) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
        verifyOwnership(user, codersUserId);
        user.unsubscribe();
        return toResponse(user, "구독이 취소되었습니다.");
    }

    @Transactional
    public UserDto.Response unsubscribeByToken(String token) {
        User user = userRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 수신 거부 링크입니다."));
        user.unsubscribe();
        return toResponse(user, "구독이 취소되었습니다.");
    }

    @Transactional
    public UserDto.Response resubscribe(String email, String codersUserId) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
        verifyOwnership(user, codersUserId);
        user.subscribe();
        return toResponse(user, "구독이 재개되었습니다!");
    }

    @Transactional
    public UserDto.Response updateLocation(
            UserDto.UpdateLocationRequest request,
            String codersUserId
    ) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
        verifyOwnership(user, codersUserId);

        user.updateLocation(
                request.getLocationName(),
                request.getLatitude(),
                request.getLongitude(),
                request.getNx(),
                request.getNy()
        );

        return toResponse(user, "구독 위치가 변경되었습니다.");
    }

    @Transactional
    public UserDto.Response updateStylePreference(
            UserDto.UpdateStylePreferenceRequest request,
            String codersUserId
    ) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
        verifyOwnership(user, codersUserId);

        user.updateStylePreference(request.getAgeGroup(), request.getGender());

        return toResponse(user, "스타일 추천 기준이 변경되었습니다.");
    }

    @Transactional
    public List<User> getSubscribedUsers() {
        List<User> users = userRepository.findAllBySubscribedTrue();
        users.forEach(User::ensureUnsubscribeToken);
        return users;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
    }

    public Optional<UserDto.Response> findCurrentSubscription(String codersUserId) {
        String normalized = requireAndNormalizeCodersIdentity(codersUserId);
        if (normalized == null) {
            return Optional.empty();
        }
        return userRepository.findByCodersUserId(normalized)
                .map(user -> toResponse(user, "내 구독 정보를 불러왔습니다."));
    }

    @Transactional
    public UserDto.Response updateNotificationTimes(
            UserDto.UpdateNotificationRequest request,
            String codersUserId
    ) {
        User user = findCurrentUser(codersUserId);
        user.updateNotificationTimes(
                request.isMorningEnabled(),
                request.isAfternoonEnabled(),
                request.isEveningEnabled()
        );
        return toResponse(user, "알림 시간이 변경되었습니다.");
    }

    @Transactional
    public UserDto.Response unsubscribeCurrent(String codersUserId) {
        User user = findCurrentUser(codersUserId);
        user.unsubscribe();
        return toResponse(user, "구독이 취소되었습니다.");
    }

    private void requireCodersIdentity(String codersUserId) {
        if (codersIdentityRequired && normalizeCodersUserId(codersUserId) == null) {
            throw new SecurityException("로그인 후 이용해주세요.");
        }
    }

    private String requireAndNormalizeCodersIdentity(String codersUserId) {
        String normalized = normalizeCodersUserId(codersUserId);
        if (normalized == null && codersIdentityRequired) {
            throw new SecurityException("로그인 후 이용해주세요.");
        }
        return normalized;
    }

    private User findCurrentUser(String codersUserId) {
        String normalized = requireAndNormalizeCodersIdentity(codersUserId);
        if (normalized == null) {
            throw new SecurityException("내 구독 관리는 coders.kr 로그인 후 이용해주세요.");
        }
        return userRepository.findByCodersUserId(normalized)
                .orElseThrow(() -> new IllegalArgumentException("현재 계정에 연결된 구독이 없습니다."));
    }

    private void verifyOwnership(User user, String codersUserId) {
        if (!codersIdentityRequired) {
            return;
        }
        String normalized = normalizeCodersUserId(codersUserId);
        if (normalized == null) {
            throw new SecurityException("로그인 후 이용해주세요.");
        }
        if (user.getCodersUserId() == null || user.getCodersUserId().isBlank()) {
            user.claimCodersIdentity(normalized);
            return;
        }
        if (!user.getCodersUserId().equals(normalized)) {
            throw new SecurityException("본인의 구독 정보만 변경할 수 있습니다.");
        }
    }

    private String normalizeCodersUserId(String codersUserId) {
        return codersUserId == null || codersUserId.isBlank()
                ? null
                : codersUserId.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        return email.trim().toLowerCase();
    }

    private UserDto.Response toResponse(User user, String message) {
        return UserDto.Response.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .subscribed(user.isSubscribed())
                .locationName(user.getLocationName())
                .ageGroup(user.getAgeGroup())
                .gender(user.getGender())
                .morningEnabled(user.isMorningEnabled())
                .afternoonEnabled(user.isAfternoonEnabled())
                .eveningEnabled(user.isEveningEnabled())
                .message(message)
                .build();
    }
}
