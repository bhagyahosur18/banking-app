package com.backendev.transactionservice.integration.service;

import com.backendev.transactionservice.service.UserInfoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceIntegrationTest {

    private UserInfoService userInfoService;

    @BeforeEach
    void setUp() {
        userInfoService = new UserInfoService();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetCurrentUserId() {
        String userId = "john-123";
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(authentication);

         String result = userInfoService.getCurrentUserId();

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void shouldThrowExceptionWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userInfoService.getCurrentUserId())
                .isInstanceOf(SecurityException.class)
                .hasMessage("User not authenticated");
    }

    @Test
    void shouldThrowExceptionWhenNotAuthenticated() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", null, new ArrayList<>());
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> userInfoService.getCurrentUserId())
                .isInstanceOf(SecurityException.class)
                .hasMessage("User not authenticated");
    }
}
