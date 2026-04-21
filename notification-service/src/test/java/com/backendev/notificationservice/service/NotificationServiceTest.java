package com.backendev.notificationservice.service;

import com.backendev.notificationservice.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    private NotificationEvent notificationEvent;

    @BeforeEach
    void setUp() {
        notificationEvent = NotificationEvent.builder()
                .eventType("USER_REGISTERED")
                .userId("USR-001")
                .email("jane@example.com")
                .subject("Welcome to BankApp")
                .message("Your account has been created successfully.")
                .build();
    }

    @Test
    void processNotification_shouldCompleteWithoutException() {
        assertDoesNotThrow(() -> notificationService.processNotification(notificationEvent));
    }

    @Test
    void processNotification_shouldHandleUserRegisteredEvent() {
        NotificationEvent event = NotificationEvent.builder()
                .eventType("USER_REGISTERED")
                .userId("USR-001")
                .email("john@example.com")
                .subject("Welcome")
                .message("Account created.")
                .build();

        assertDoesNotThrow(() -> notificationService.processNotification(event));
    }

    @Test
    void processNotification_shouldHandleNullFieldsWithoutException() {
        NotificationEvent eventWithNulls = NotificationEvent.builder()
                .eventType("USER_REGISTERED")
                .userId(null)
                .email(null)
                .subject(null)
                .message(null)
                .build();

        assertDoesNotThrow(() -> notificationService.processNotification(eventWithNulls));
    }

}