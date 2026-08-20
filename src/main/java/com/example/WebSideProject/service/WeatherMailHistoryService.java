package com.example.WebSideProject.service;

import com.example.WebSideProject.Enum.MailSendStatus;
import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.dto.WeatherMailHistoryDto;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.entity.WeatherMailHistory;
import com.example.WebSideProject.repository.WeatherMailHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeatherMailHistoryService {

    private final WeatherMailHistoryRepository weatherMailHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(User user, WeatherDto weather) {
        save(user, weather, MailSendStatus.SUCCESS, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(User user, WeatherDto weather, Exception exception) {
        save(user, weather, MailSendStatus.FAILED, getErrorMessage(exception));
    }

    public List<WeatherMailHistoryDto.Response> getRecentHistories(String email) {
        List<WeatherMailHistory> histories = email == null || email.isBlank()
                ? weatherMailHistoryRepository.findTop50ByOrderBySentAtDesc()
                : weatherMailHistoryRepository.findTop50ByUserEmailOrderBySentAtDesc(email);

        return histories.stream()
                .map(WeatherMailHistoryDto.Response::from)
                .toList();
    }

    private void save(User user, WeatherDto weather, MailSendStatus status, String errorMessage) {
        WeatherPeriod period = WeatherPeriod.MORNING;
        try {
            period = WeatherPeriod.fromLabel(weather.getPeriodLabel());
        } catch (Exception ignored) {
        }

        weatherMailHistoryRepository.save(WeatherMailHistory.builder()
                .userEmail(user.getEmail())
                .locationName(user.getLocationName())
                .period(period)
                .status(status)
                .forecastDate(weather.getDate())
                .forecastTime(weather.getTime())
                .errorMessage(errorMessage)
                .build());
    }

    private String getErrorMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return "알 수 없는 메일 발송 오류";
        }
        return exception.getMessage().length() > 1000
                ? exception.getMessage().substring(0, 1000)
                : exception.getMessage();
    }
}
