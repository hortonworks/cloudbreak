package com.sequenceiq.cloudbreak.service.notification;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventBaseV4;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.notification.Notification;
import com.sequenceiq.cloudbreak.notification.NotificationSender;

@Service
public class HttpNotificationSender implements NotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotificationSender.class);

    private static final String TEST_NOTIFICATION_TYPE = "TEST_NOTIFICATION";

    @Value("${cb.notification.endpoint}")
    private String cbNotificationEndpoint;

    private final Client restClient = RestClientUtil.get(new ConfigKey(false, false, false));

    @Override
    public <T> void send(Notification<T> notification) {
        try {
            restClient
                    .target(cbNotificationEndpoint)
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .post(Entity.json(notification.getNotification()), String.class);
        } catch (Exception ex) {
            LOGGER.info("Could not send notification to the specified endpoint: '{}' Cause: {}", cbNotificationEndpoint, ex.getMessage());
        }
    }

    @Override
    public void sendTestNotification(String userId) {
        LOGGER.debug("Sending test notification to the specified endpoint: '{}'", cbNotificationEndpoint);
        try {
            restClient
                    .target(cbNotificationEndpoint)
                    .request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .post(Entity.json(createTestNotification(userId)), String.class);
        } catch (Exception ex) {
            LOGGER.info("Could not send notification to the specified endpoint: '{}' Cause: {}", cbNotificationEndpoint, ex.getMessage());
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
    }
}
