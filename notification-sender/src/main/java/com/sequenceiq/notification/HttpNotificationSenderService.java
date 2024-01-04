package com.sequenceiq.notification;

import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HttpNotificationSenderService implements NotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotificationSenderService.class);

    @Override
    public <T> void send(Notification<T> notification, List<String> endpoints, Client restClient) {
        for (String endpoint : endpoints) {
            try {
                restClient
                        .target(endpoint)
                        .request()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .post(Entity.json(notification.getNotification()), String.class);
            } catch (Exception ex) {
                LOGGER.info("Could not send notification to the specified endpoint: '{}' Cause: {}", endpoint, ex.getMessage());
            }
        }
    }
}
