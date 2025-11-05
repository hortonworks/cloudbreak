package com.sequenceiq.notification;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;

@Service
public class HttpWebSocketNotificationSenderService implements WebSocketNotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpWebSocketNotificationSenderService.class);

    private static final String NOTIFICATION_SIZE_EXCEEDED = "notification.size.exceeded";

    @Value("${notification.max.size.byte:5242880}")
    private Long maxNotificationSize;

    @Inject
    private ObjectMapper objectMapper;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Override
    public <T> void send(Notification<T> notification, List<String> endpoints, Client restClient) {
        try {
            String notificationAsString = objectMapper.writeValueAsString(notification.getNotification());
            if (notificationAsString.getBytes().length > maxNotificationSize) {
                metricService.incrementMetricCounter(NOTIFICATION_SIZE_EXCEEDED);
                LOGGER.warn("Notification size exceeded the limit: {}. Notification: {}", maxNotificationSize, notificationAsString);
            }
            for (String endpoint : endpoints) {
                try {
                    restClient
                            .target(endpoint)
                            .request()
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                            .post(Entity.json(notificationAsString), String.class);
                } catch (Exception ex) {
                    LOGGER.info("Could not send notification to the specified endpoint: '{}' Cause: {}", endpoint, ex.getMessage());
                }
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialize notification: {}", notification.getNotification());
        }
    }

}
