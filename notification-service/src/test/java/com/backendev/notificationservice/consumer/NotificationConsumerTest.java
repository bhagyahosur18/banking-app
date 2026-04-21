package com.backendev.notificationservice.consumer;

import com.backendev.notificationservice.dto.NotificationEvent;
import com.backendev.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    private String validUserEventJson;
    private String validAccountEventJson;
    private String validTransactionEventJson;

    @BeforeEach
    void setUp() {
        validUserEventJson = """
                {
                  "eventType": "USER_REGISTERED",
                  "userId": "USR-001",
                  "email": "jane@example.com",
                  "subject": "Welcome to BankApp",
                  "message": "Your account has been created successfully."
                }
                """;

        validAccountEventJson = """
                {
                  "eventType": "ACCOUNT_CREATED",
                  "userId": "USR-001",
                  "email": "jane@example.com",
                  "subject": "Account Created",
                  "message": "Your bank account has been created."
                }
                """;

        validTransactionEventJson = """
                {
                  "eventType": "TRANSACTION_COMPLETED",
                  "userId": "USR-001",
                  "email": "jane@example.com",
                  "subject": "Transaction Alert",
                  "message": "Your account was debited $500.00"
                }
                """;
    }

    @Test
    void consumeUserEvent_shouldProcessSuccessfully() {
        notificationConsumer.consumeUserEvent(validUserEventJson);

        verify(notificationService, times(1)).processNotification(any(NotificationEvent.class));
    }

    @Test
    void consumeUserEvent_shouldPassCorrectEventToService() {
        notificationConsumer.consumeUserEvent(validUserEventJson);

        verify(notificationService).processNotification(argThat(event ->
                "USER_REGISTERED".equals(event.getEventType()) &&
                        "USR-001".equals(event.getUserId()) &&
                        "jane@example.com".equals(event.getEmail())
        ));
    }

    @Test
    void consumeAccountEvent_shouldProcessSuccessfully() {
        notificationConsumer.consumeAccountEvent(validAccountEventJson);

        verify(notificationService, times(1)).processNotification(any(NotificationEvent.class));
    }

    @Test
    void consumeAccountEvent_shouldPassCorrectEventToService() {
        notificationConsumer.consumeAccountEvent(validAccountEventJson);

        verify(notificationService).processNotification(argThat(event ->
                "ACCOUNT_CREATED".equals(event.getEventType()) &&
                        "USR-001".equals(event.getUserId())
        ));
    }

    @Test
    void consumeTransactionEvent_shouldProcessSuccessfully() {
        notificationConsumer.consumeTransactionEvent(validTransactionEventJson);

        verify(notificationService, times(1)).processNotification(any(NotificationEvent.class));
    }

    @Test
    void consumeTransactionEvent_shouldPassCorrectEventToService() {
        notificationConsumer.consumeTransactionEvent(validTransactionEventJson);

        verify(notificationService).processNotification(argThat(event ->
                "TRANSACTION_COMPLETED".equals(event.getEventType()) &&
                        "USR-001".equals(event.getUserId())
        ));
    }

    @Test
    void consumeUserEvent_shouldHandleMalformedJsonGracefully() {
        notificationConsumer.consumeUserEvent("{ not valid json }");

        verify(notificationService, never()).processNotification(any());
    }

    @Test
    void consumeUserEvent_shouldHandleEmptyMessageGracefully() {
        notificationConsumer.consumeUserEvent("");

        verify(notificationService, never()).processNotification(any());
    }

    @Test
    void consumeUserEvent_shouldHandleNullFieldsInEvent() {
        String jsonWithNullFields = """
                {
                  "eventType": "USER_REGISTERED",
                  "userId": null,
                  "email": null,
                  "subject": null,
                  "message": null
                }
                """;

        notificationConsumer.consumeUserEvent(jsonWithNullFields);

        verify(notificationService, times(1)).processNotification(any(NotificationEvent.class));
    }
}