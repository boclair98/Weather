package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.UserDto;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void registersEachDistinctRecipientFromCommaAndNewlineInput() {
        UserRepository repository = mock(UserRepository.class);
        WeatherService weatherService = mock(WeatherService.class);
        MailService mailService = mock(MailService.class);
        UserService service = new UserService(repository, weatherService, mailService);
        UserDto.RegisterRequest request = request("first@example.com, second@example.com\nfirst@example.com");

        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(weatherService.getWeather(anyInt(), anyInt(), any(), anyString()))
                .thenReturn(WeatherDto.builder().date("20260802").time("0900").periodLabel("아침")
                        .sky("1").pty("0").tmp("24").pop("0").reh("50").wsd("1").build());

        List<UserDto.Response> responses = service.registerAll(request, "one-platform-user");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(UserDto.Response::getEmail)
                .containsExactly("first@example.com", "second@example.com");
        ArgumentCaptor<User> savedUsers = ArgumentCaptor.forClass(User.class);
        verify(repository, times(2)).save(savedUsers.capture());
        assertThat(savedUsers.getAllValues()).extracting(User::getCodersUserId).containsOnlyNulls();
        verify(mailService, times(2)).sendWeatherMail(any(User.class), any(WeatherDto.class));
    }

    @Test
    void rejectsInvalidRecipientBeforeSaving() {
        UserService service = new UserService(mock(UserRepository.class), mock(WeatherService.class), mock(MailService.class));

        assertThatThrownBy(() -> service.registerAll(request("not-an-email"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("올바른 이메일");
    }

    private UserDto.RegisterRequest request(String emails) {
        UserDto.RegisterRequest request = new UserDto.RegisterRequest();
        request.setName("날씨 사용자");
        request.setEmail(emails);
        request.setLocationName("서울특별시 중구");
        request.setNx(60);
        request.setNy(127);
        return request;
    }
}
