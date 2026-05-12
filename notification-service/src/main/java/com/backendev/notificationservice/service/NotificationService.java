package com.backendev.notificationservice.service;

import com.backendev.notificationservice.dto.NotificationEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationService {

    private final EmailService emailService;

    public void processNotification(NotificationEvent event) {
        log.info("===== NOTIFICATION =====");
        log.info("Type    : {}", event.getEventType());
        log.info("User ID : {}", event.getUserId());
        log.info("Email   : {}", event.getEmail());
        log.info("Subject : {}", event.getSubject());
        log.info("Message : {}", event.getMessage());

        try {
            emailService.sendEmail(event);
        } catch (Exception e) {
            log.error("Failed to process notification for user: {} | Error: {}",
                    event.getUserId(), e.getMessage());
        }
    }
}
