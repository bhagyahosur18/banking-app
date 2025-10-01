package com.backendev.transactionservice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceTest {
    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserInfoService userInfoService;

    private static final String USER_ID = "user123";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_AuthenticatedUser_ReturnsUserId() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(USER_ID);

        String result = userInfoService.getCurrentUserId();

        assertEquals(USER_ID, result);
        verify(securityContext).getAuthentication();
        verify(authentication).isAuthenticated();
        verify(authentication).getName();
    }

    @Test
    void getCurrentUserId_NullAuthentication_ThrowsSecurityException() {
        when(securityContext.getAuthentication()).thenReturn(null);

        SecurityException exception = assertThrows(SecurityException.class,
                () -> userInfoService.getCurrentUserId());

        assertEquals("User not authenticated", exception.getMessage());
        verify(securityContext).getAuthentication();
    }

    @Test
    void getCurrentUserId_NotAuthenticated_ThrowsSecurityException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        SecurityException exception = assertThrows(SecurityException.class,
                () -> userInfoService.getCurrentUserId());

        assertEquals("User not authenticated", exception.getMessage());
        verify(securityContext).getAuthentication();
        verify(authentication).isAuthenticated();
        verify(authentication, never()).getName();
    }

}