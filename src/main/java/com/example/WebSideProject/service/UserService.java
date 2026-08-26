package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.UserDto;
import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.event.SubscriptionWelcomeMailRequested;
import com.example.WebSideProject.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final int MAX_RECIPIENTS = 10;
    private static final String PRIVACY_CONSENT_VERSION = "2026-08-09";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailVerificationService emailVerificationService;

    @Autowired
    public UserService(
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.emailVerificationService = emailVerificationService;
    }

    public UserService(UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this(userRepository, eventPublisher, null);
    }

    @Value("${coders.identity.required:false}")
    private boolean codersIdentityRequired;

    @Transactional
    public UserDto.Response register(UserDto.RegisterRequest request, String codersUserId) {
        return registerAll(request, codersUserId).get(0);
    }

    @Transactional
    public List<UserDto.Response> registerAll(UserDto.RegisterRequest request, String codersUserId) {
        requireCodersIdentity(codersUserId);
        if (codersIdentityRequired) {
            if (emailVerificationService == null) {
                throw new IllegalStateException("이메일 인증 기능이 준비되지 않았습니다.");
            }
            List<String> requestedEmails = extractEmails(request);
            if (requestedEmails.size() != 1) {
                throw new SecurityException("로그인 계정에는 인증된 이메일 하나만 연결할 수 있습니다.");
            }
            String verifiedEmail = emailVerificationService.consumeVerifiedEmail(
                    normalizeCodersUserId(codersUserId),
                    request.getVerificationToken(),
                    requestedEmails.get(0)
            );
            ensureSingleVerifiedEmail(normalizeCodersUserId(codersUserId), verifiedEmail);
            return List.of(registerSingle(request, verifiedEmail, codersUserId));
        }
        return extractEmails(request).stream()
                .map(email -> registerSingle(request, email, codersUserId))
                .toList();
    }

    private UserDto.Response registerSingle(UserDto.RegisterRequest request, String email, String codersUserId) {
        String identity = codersIdentityRequired ? normalizeCodersUserId(codersUserId) : null;
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (identity != null) {
                verifyOwnership(user, codersUserId);
            }
            user.updateName(request.getName());
            user.updateLocation(
                    request.getLocationName(),
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getNx(),
                    request.getNy()
            );
            user.updateStylePreference(
                    request.getAgeGroup(),
                    request.getGender(),
                    request.getTemperatureSensitivity(),
                    request.getActivityType()
            );
            user.updateSmartAlerts(
                    request.isSmartAlertEnabled(),
                    request.isRainAlertEnabled(),
                    request.isTemperatureAlertEnabled(),
                    request.isAirQualityAlertEnabled(),
                    request.isWindAlertEnabled()
            );
            user.updateNotificationTimes(
                    request.isMorningEnabled(),
                    request.isAfternoonEnabled(),
                    request.isEveningEnabled(),
                    request.getMorningTime(),
                    request.getAfternoonTime(),
                    request.getEveningTime()
            );
            user.recordPrivacyConsent(PRIVACY_CONSENT_VERSION);
            user.subscribe();
            eventPublisher.publishEvent(new SubscriptionWelcomeMailRequested(user));
            return toResponse(user, "구독 정보가 새롭게 업데이트되었습니다.");
        }

        boolean hasNotificationTime = request.isMorningEnabled()
                || request.isAfternoonEnabled()
                || request.isEveningEnabled();
        boolean morningEnabled = request.isMorningEnabled() || !hasNotificationTime;

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .ownerId(identity)
                .locationName(request.getLocationName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .nx(request.getNx())
                .ny(request.getNy())
                .ageGroup(request.getAgeGroup())
                .gender(request.getGender())
                .temperatureSensitivity(request.getTemperatureSensitivity())
                .activityType(request.getActivityType())
                .smartAlertEnabled(request.isSmartAlertEnabled())
                .rainAlertEnabled(request.isRainAlertEnabled())
                .temperatureAlertEnabled(request.isTemperatureAlertEnabled())
                .airQualityAlertEnabled(request.isAirQualityAlertEnabled())
                .windAlertEnabled(request.isWindAlertEnabled())
                .morningEnabled(morningEnabled)
                .afternoonEnabled(request.isAfternoonEnabled())
                .eveningEnabled(request.isEveningEnabled())
                .morningTime(request.getMorningTime())
                .afternoonTime(request.getAfternoonTime())
                .eveningTime(request.getEveningTime())
                .build();
        user.recordPrivacyConsent(PRIVACY_CONSENT_VERSION);

        User saved = userRepository.save(user);
        log.info("신규 구독자 등록: userId={}", saved.getId());
        eventPublisher.publishEvent(new SubscriptionWelcomeMailRequested(saved));

        return UserDto.Response.builder()
                .id(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .subscribed(saved.isSubscribed())
                .locationName(saved.getLocationName())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .nx(saved.getNx())
                .ny(saved.getNy())
                .ageGroup(saved.getAgeGroup())
                .gender(saved.getGender())
                .temperatureSensitivity(saved.getTemperatureSensitivity())
                .activityType(saved.getActivityType())
                .smartAlertEnabled(saved.isSmartAlertEnabled())
                .rainAlertEnabled(saved.isRainAlertEnabled())
                .temperatureAlertEnabled(saved.isTemperatureAlertEnabled())
                .airQualityAlertEnabled(saved.isAirQualityAlertEnabled())
                .windAlertEnabled(saved.isWindAlertEnabled())
                .morningEnabled(saved.isMorningEnabled())
                .afternoonEnabled(saved.isAfternoonEnabled())
                .eveningEnabled(saved.isEveningEnabled())
                .morningTime(saved.getMorningTime())
                .afternoonTime(saved.getAfternoonTime())
                .eveningTime(saved.getEveningTime())
                .message("구독이 완료되었습니다! 선택한 시간에 날씨를 보내드릴게요 🌤️")
                .build();
    }

    private List<String> extractEmails(UserDto.RegisterRequest request) {
        Set<String> recipients = new LinkedHashSet<>();
        addEmails(recipients, request.getEmail());
        if (request.getEmails() != null) {
            request.getEmails().forEach(email -> addEmails(recipients, email));
        }
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("이메일을 하나 이상 입력해주세요.");
        }
        if (recipients.size() > MAX_RECIPIENTS) {
            throw new IllegalArgumentException("한 번에 최대 10개의 이메일을 등록할 수 있습니다.");
        }
        return List.copyOf(recipients);
    }

    private void addEmails(Set<String> recipients, String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        for (String candidate : input.split("[,;\\s]+")) {
            if (candidate.isBlank()) {
                continue;
            }
            String email = normalizeEmail(candidate);
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다: " + candidate);
            }
            recipients.add(email);
        }
    }

    private void ensureSingleVerifiedEmail(String ownerId, String verifiedEmail) {
        Optional<User> existingSubscription = userRepository.findFirstByOwnerIdOrderByIdAsc(ownerId)
                .or(() -> userRepository.findByCodersUserId(ownerId));
        if (existingSubscription.isPresent()
                && !existingSubscription.get().getEmail().equalsIgnoreCase(verifiedEmail)) {
            throw new SecurityException("로그인 계정에는 이메일 하나만 연결할 수 있습니다. 기존 이메일로 구독하거나 내 데이터 완전 삭제 후 다시 시도해주세요.");
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
    public List<UserDto.Response> unsubscribeAll(UserDto.UnsubscribeRequest request, String codersUserId) {
        Set<String> recipients = new LinkedHashSet<>();
        addEmails(recipients, request.getEmail());
        if (request.getEmails() != null) {
            request.getEmails().forEach(email -> addEmails(recipients, email));
        }
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("구독을 취소할 이메일을 하나 이상 입력해주세요.");
        }
        if (recipients.size() > MAX_RECIPIENTS) {
            throw new IllegalArgumentException("한 번에 최대 10개의 이메일을 처리할 수 있습니다.");
        }
        return recipients.stream()
                .map(email -> unsubscribe(email, codersUserId))
                .toList();
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

        user.updateStylePreference(
                request.getAgeGroup(),
                request.getGender(),
                request.getTemperatureSensitivity(),
                request.getActivityType()
        );

        return toResponse(user, "스타일 추천 기준이 변경되었습니다.");
    }

    @Transactional
    public List<User> getSubscribedUsers() {
        List<User> users = userRepository.findAllBySubscribedTrue();
        users.forEach(User::ensureUnsubscribeToken);
        return users;
    }

    public List<User> getDueSubscribedUsers(WeatherPeriod period, LocalTime time) {
        return switch (period) {
            case MORNING -> userRepository.findDueMorningSubscribers(time);
            case AFTERNOON -> userRepository.findDueAfternoonSubscribers(time);
            case EVENING -> userRepository.findDueEveningSubscribers(time);
        };
    }

    @Transactional
    public boolean claimScheduledMail(Long userId, WeatherPeriod period, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return user.claimScheduledMail(period, date);
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
        return findOwnedSubscription(normalized)
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
                request.isEveningEnabled(),
                request.getMorningTime(),
                request.getAfternoonTime(),
                request.getEveningTime()
        );
        return toResponse(user, "알림 시간이 변경되었습니다.");
    }

    @Transactional
    public UserDto.Response unsubscribeCurrent(String codersUserId) {
        User user = findCurrentUser(codersUserId);
        user.unsubscribe();
        return toResponse(user, "구독이 취소되었습니다.");
    }

    @Transactional
    public void deleteCurrentData(String codersUserId) {
        User user = findCurrentUser(codersUserId);
        userRepository.deleteMailHistoriesByEmail(user.getEmail());
        userRepository.delete(user);
        log.info("사용자 요청에 따라 구독 개인정보 완전 삭제: userId={}", user.getId());
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
        return findOwnedSubscription(normalized)
                .orElseThrow(() -> new IllegalArgumentException("현재 계정에 연결된 구독이 없습니다."));
    }

    @Transactional
    public UserDto.Response updateSmartAlerts(
            UserDto.UpdateSmartAlertRequest request,
            String codersUserId
    ) {
        User user = findCurrentUser(codersUserId);
        user.updateSmartAlerts(
                request.isSmartAlertEnabled(),
                request.isRainAlertEnabled(),
                request.isTemperatureAlertEnabled(),
                request.isAirQualityAlertEnabled(),
                request.isWindAlertEnabled()
        );
        return toResponse(user, "스마트 위험 알림 설정이 변경되었습니다.");
    }

    @Transactional
    public boolean markSmartAlertSent(Long userId, String fingerprint) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        if (user.hasReceivedSmartAlert(fingerprint)) {
            return false;
        }
        user.markSmartAlertSent(fingerprint);
        return true;
    }

    private Optional<User> findOwnedSubscription(String codersUserId) {
        return userRepository.findFirstByOwnerIdOrderByIdAsc(codersUserId)
                .or(() -> userRepository.findByCodersUserId(codersUserId));
    }

    private void verifyOwnership(User user, String codersUserId) {
        if (!codersIdentityRequired) {
            return;
        }
        String normalized = normalizeCodersUserId(codersUserId);
        if (normalized == null) {
            throw new SecurityException("로그인 후 이용해주세요.");
        }
        if ((user.getOwnerId() == null || user.getOwnerId().isBlank())
                && (user.getCodersUserId() == null || user.getCodersUserId().isBlank())) {
            user.claimCodersIdentity(normalized);
            return;
        }
        if (!user.isOwnedBy(normalized)) {
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
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .nx(user.getNx())
                .ny(user.getNy())
                .ageGroup(user.getAgeGroup())
                .gender(user.getGender())
                .temperatureSensitivity(user.getTemperatureSensitivity())
                .activityType(user.getActivityType())
                .smartAlertEnabled(user.isSmartAlertEnabled())
                .rainAlertEnabled(user.isRainAlertEnabled())
                .temperatureAlertEnabled(user.isTemperatureAlertEnabled())
                .airQualityAlertEnabled(user.isAirQualityAlertEnabled())
                .windAlertEnabled(user.isWindAlertEnabled())
                .morningEnabled(user.isMorningEnabled())
                .afternoonEnabled(user.isAfternoonEnabled())
                .eveningEnabled(user.isEveningEnabled())
                .morningTime(user.getMorningTime())
                .afternoonTime(user.getAfternoonTime())
                .eveningTime(user.getEveningTime())
                .privacyConsentVersion(user.getPrivacyConsentVersion())
                .privacyConsentAt(user.getPrivacyConsentAt())
                .message(message)
                .build();
    }
}
