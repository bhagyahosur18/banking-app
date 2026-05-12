package com.backendev.notificationservice.service;

import com.backendev.notificationservice.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailService emailService;

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
    void processNotification_shouldDelegateToEmailService() {
        notificationService.processNotification(notificationEvent);

        verify(emailService, times(1)).sendEmail(notificationEvent);
    }

    @Test
    void processNotification_shouldPassCorrectEventToEmailService() {
        notificationService.processNotification(notificationEvent);

        verify(emailService).sendEmail(argThat(e ->
                "USER_REGISTERED".equals(e.getEventType()) &&
                        "jane@example.com".equals(e.getEmail()) &&
                        "Welcome to BankApp".equals(e.getSubject())
        ));
    }

    @Test
    void processNotification_shouldHandleAllEventTypes() {
        String[] eventTypes = {
                "USER_REGISTERED", "USER_UPDATED",
                "ACCOUNT_CREATED", "ACCOUNT_DELETED",
                "TRANSACTION_COMPLETED", "TRANSACTION_FAILED"
        };

        for (String eventType : eventTypes) {
            NotificationEvent e = NotificationEvent.builder()
                    .eventType(eventType)
                    .userId("USR-001")
                    .email("jane@example.com")
                    .subject("Test")
                    .message("Test message")
                    .build();

            assertDoesNotThrow(() -> notificationService.processNotification(e));
        }

        verify(emailService, times(eventTypes.length)).sendEmail(any());
    }

    @Test
    void processNotification_shouldNotThrow_whenEmailServiceFails() {
        doThrow(new RuntimeException("Email service down"))
                .when(emailService).sendEmail(any());

        assertDoesNotThrow(() -> notificationService.processNotification(notificationEvent));
    }

    @Test
    void processNotification_shouldHandleNullFields() {
        NotificationEvent eventWithNulls = NotificationEvent.builder()
                .eventType("USER_REGISTERED")
                .userId(null)
                .email(null)
                .subject(null)
                .message(null)
                .build();

        assertDoesNotThrow(() -> notificationService.processNotification(eventWithNulls));
        verify(emailService, times(1)).sendEmail(eventWithNulls);
    }

}