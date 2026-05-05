package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.UserDto;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final WeatherService weatherService;
    private final MailService mailService;

    @Transactional
    public UserDto.Response register(UserDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다: " + request.getEmail());
        }

        boolean hasNotificationTime = request.isMorningEnabled()
                || request.isAfternoonEnabled()
                || request.isEveningEnabled();
        boolean morningEnabled = request.isMorningEnabled() || !hasNotificationTime;

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
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
            WeatherDto weather = weatherService.getWeather(user.getNx(), user.getNy());
            mailService.sendWeatherMail(user, weather);
        } catch (Exception e) {
            log.error("구독 직후 날씨 메일 발송 준비 실패: {}", user.getEmail(), e);
        }
    }

    @Transactional
    public UserDto.Response unsubscribe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
        user.unsubscribe();
        return UserDto.Response.builder()
                .email(email)
                .subscribed(false)
                .locationName(user.getLocationName())
                .ageGroup(user.getAgeGroup())
                .gender(user.getGender())
                .morningEnabled(user.isMorningEnabled())
                .afternoonEnabled(user.isAfternoonEnabled())
                .eveningEnabled(user.isEveningEnabled())
                .message("구독이 취소되었습니다.")
                .build();
    }

    @Transactional
    public UserDto.Response resubscribe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
        user.subscribe();
        return UserDto.Response.builder()
                .email(email)
                .subscribed(true)
                .locationName(user.getLocationName())
                .ageGroup(user.getAgeGroup())
                .gender(user.getGender())
                .morningEnabled(user.isMorningEnabled())
                .afternoonEnabled(user.isAfternoonEnabled())
                .eveningEnabled(user.isEveningEnabled())
                .message("구독이 재개되었습니다!")
                .build();
    }

    @Transactional
    public UserDto.Response updateLocation(UserDto.UpdateLocationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        user.updateLocation(
                request.getLocationName(),
                request.getLatitude(),
                request.getLongitude(),
                request.getNx(),
                request.getNy()
        );

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
                .message("구독 위치가 변경되었습니다.")
                .build();
    }

    @Transactional
    public UserDto.Response updateStylePreference(UserDto.UpdateStylePreferenceRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        user.updateStylePreference(request.getAgeGroup(), request.getGender());

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
                .message("스타일 추천 기준이 변경되었습니다.")
                .build();
    }

    public List<User> getSubscribedUsers() {
        return userRepository.findAllBySubscribedTrue();
    }
}
