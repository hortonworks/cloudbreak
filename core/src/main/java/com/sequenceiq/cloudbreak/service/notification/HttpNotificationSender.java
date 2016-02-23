package com.sequenceiq.cloudbreak.service.notification;

import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;

@Service
public class HttpNotificationSender implements NotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotificationSender.class);
    private static final String JSON_CONTENT_TYPE = "application/json";

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private Client restClient;

    @Override
    public void send(Notification notification) {
        List<Subscription> subscriptions = (List<Subscription>) subscriptionRepository.findAll();
        for (Subscription subscription : subscriptions) {
            String endpoint = subscription.getEndpoint();
            try {
                restClient
                        .target(endpoint)
                        .request()
                        .header(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
                        .post(Entity.json(notification), String.class);
            } catch (Exception ex) {
                LOGGER.info("Could not send notification to the specified endpoint: '{}' Cause: {}", endpoint, ex.getMessage());
            }
        }
    }
}
