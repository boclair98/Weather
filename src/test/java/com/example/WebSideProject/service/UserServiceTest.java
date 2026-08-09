package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.UserDto;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.event.SubscriptionWelcomeMailRequested;
import com.example.WebSideProject.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void registersEachDistinctRecipientFromCommaAndNewlineInput() {
        UserRepository repository = mock(UserRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        UserService service = new UserService(repository, eventPublisher);
        UserDto.RegisterRequest request = request("first@example.com, second@example.com\nfirst@example.com");

        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<UserDto.Response> responses = service.registerAll(request, "one-platform-user");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(UserDto.Response::getEmail)
                .containsExactly("first@example.com", "second@example.com");
        ArgumentCaptor<User> savedUsers = ArgumentCaptor.forClass(User.class);
        verify(repository, times(2)).save(savedUsers.capture());
        assertThat(savedUsers.getAllValues()).extracting(User::getCodersUserId).containsOnlyNulls();
        verify(eventPublisher, times(2)).publishEvent(any(SubscriptionWelcomeMailRequested.class));
    }

    @Test
    void rejectsInvalidRecipientBeforeSaving() {
        UserService service = new UserService(mock(UserRepository.class), mock(ApplicationEventPublisher.class));

        assertThatThrownBy(() -> service.registerAll(request("not-an-email"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("올바른 이메일");
    }

    @Test
    void unsubscribesEveryDistinctEmail() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository, mock(ApplicationEventPublisher.class));
        User first = User.builder().name("첫 번째").email("first@example.com").nx(60).ny(127).build();
        User second = User.builder().name("두 번째").email("second@example.com").nx(60).ny(127).build();
        when(repository.findByEmail("first@example.com")).thenReturn(Optional.of(first));
        when(repository.findByEmail("second@example.com")).thenReturn(Optional.of(second));

        UserDto.UnsubscribeRequest request = new UserDto.UnsubscribeRequest();
        request.setEmail("first@example.com");
        request.setEmails(List.of("first@example.com", "second@example.com"));

        List<UserDto.Response> responses = service.unsubscribeAll(request, null);

        assertThat(responses).extracting(UserDto.Response::getEmail)
                .containsExactly("first@example.com", "second@example.com");
        assertThat(first.isSubscribed()).isFalse();
        assertThat(second.isSubscribed()).isFalse();
    }

    @Test
    void oneValidatedIdentityCanOwnMultipleRecipients() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository, mock(ApplicationEventPublisher.class));
        ReflectionTestUtils.setField(service, "codersIdentityRequired", true);
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<UserDto.Response> responses = service.registerAll(
                request("first@example.com, second@example.com"),
                "validated-google-user"
        );

        assertThat(responses).hasSize(2);
        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(repository, times(2)).save(users.capture());
        assertThat(users.getAllValues()).extracting(User::getOwnerId)
                .containsOnly("validated-google-user");
        assertThat(users.getAllValues()).extracting(User::getCodersUserId)
                .containsOnlyNulls();
    }

    @Test
    void validatedIdentityCannotReplaceAnotherOwnersSubscription() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository, mock(ApplicationEventPublisher.class));
        ReflectionTestUtils.setField(service, "codersIdentityRequired", true);
        User owned = User.builder()
                .name("기존 사용자")
                .email("owned@example.com")
                .ownerId("original-owner")
                .locationName("서울")
                .nx(60)
                .ny(127)
                .build();
        when(repository.findByEmail("owned@example.com")).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> service.registerAll(request("owned@example.com"), "different-owner"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("본인의 구독");
    }

    @Test
    void deletesCurrentUsersStoredPersonalData() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository, mock(ApplicationEventPublisher.class));
        ReflectionTestUtils.setField(service, "codersIdentityRequired", true);
        User owned = User.builder()
                .name("삭제 사용자")
                .email("delete@example.com")
                .ownerId("validated-user")
                .nx(60)
                .ny(127)
                .build();
        when(repository.findFirstByOwnerIdOrderByIdAsc("validated-user"))
                .thenReturn(Optional.of(owned));

        service.deleteCurrentData("validated-user");

        verify(repository).deleteMailHistoriesByEmail("delete@example.com");
        verify(repository).delete(owned);
    }

    private UserDto.RegisterRequest request(String emails) {
        UserDto.RegisterRequest request = new UserDto.RegisterRequest();
        request.setName("날씨 사용자");
        request.setEmail(emails);
        request.setLocationName("서울특별시 중구");
        request.setNx(60);
        request.setNy(127);
        request.setPrivacyConsent(true);
        return request;
    }
}
