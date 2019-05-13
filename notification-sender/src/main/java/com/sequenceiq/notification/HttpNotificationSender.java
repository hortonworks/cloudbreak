package com.sequenceiq.notification;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HttpNotificationSender implements NotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotificationSender.class);

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

    /*@Override
    public void sendTestNotification(String userId, List<String> endpoints, Client restClient) {
        Iterable<Subscription> subscriptions = subscriptionService.findAll();
        for (Subscription subscription : subscriptions) {
            String endpoint = subscription.getEndpoint();
            LOGGER.debug("Sending test notification to the specified endpoint: '{}'", endpoint);
            try {
                restClient
                        .target(endpoint)
                        .request()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .post(Entity.json(createTestNotification(userId)), String.class);
            } catch (Exception ex) {
                LOGGER.info("Could not send notification to the specified endpoint: '{}' Cause: {}", endpoint, ex.getMessage());
            }
        }
    }

    private CloudbreakEventBaseV4 createTestNotification(String userId) {
        CloudbreakEventBaseV4 baseEvent = new CloudbreakEventBaseV4();
        baseEvent.setEventType(TEST_NOTIFICATION_TYPE);
        baseEvent.setEventMessage("Test notification message.");
        baseEvent.setEventTimestamp(System.currentTimeMillis());
        baseEvent.setUserId(userId);
        baseEvent.setNotificationType(TEST_NOTIFICATION_TYPE);
        return baseEvent;
    }*/
}
