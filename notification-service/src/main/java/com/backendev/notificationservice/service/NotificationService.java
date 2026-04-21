package com.backendev.notificationservice.service;

import com.backendev.notificationservice.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    public void processNotification(NotificationEvent event) {
        log.info("===== NOTIFICATION =====");
        log.info("Type    : {}", event.getEventType());
        log.info("User ID : {}", event.getUserId());
        log.info("Email   : {}", event.getEmail());
        log.info("Subject : {}", event.getSubject());
        log.info("Message : {}", event.getMessage());
        sendEmail(event);
    }

    private void sendEmail(NotificationEvent event) {
        // Placeholder — integrate any email provider here
        log.info("[EMAIL] To: {} | Subject: {} | Body: {}",
                event.getEmail(), event.getSubject(), event.getMessage());
    }
}
