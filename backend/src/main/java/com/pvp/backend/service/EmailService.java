package com.pvp.backend.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendNotification(Long orderId, String eventType) {
        System.out.println("[EmailService] sendNotification called for orderId=" + orderId + ", event=" + eventType);
    }

    public void sendEmail(String recipient, String subject, String body) {
        System.out.println("[EmailService] sendEmail called: recipient=" + recipient + ", subject=" + subject);
    }
}
