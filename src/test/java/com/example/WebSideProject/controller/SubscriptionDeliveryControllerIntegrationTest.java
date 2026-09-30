package com.example.WebSideProject.controller;

import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionDeliveryControllerIntegrationTest {

    private static final String TEST_EMAIL = "delivery-overview-test@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    void ownerCanReadOwnNextDeliveryButAnonymousVisitorCannot() throws Exception {
        userRepository.save(User.builder()
                .name("테스트 구독자")
                .email(TEST_EMAIL)
                .ownerId("delivery-owner-test")
                .locationName("서울 강남역")
                .nx(61).ny(125)
                .morningEnabled(true)
                .morningTime(LocalTime.of(7, 30))
                .build());

        mockMvc.perform(get("/api/users/me/delivery")
                        .header("X-Coders-User", "delivery-owner-test"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.nextScheduledAt", notNullValue()))
                .andExpect(jsonPath("$.timezone").value("Asia/Seoul"));

        mockMvc.perform(get("/api/users/me/delivery"))
                .andExpect(status().isForbidden());
    }
}
