package com.sequenceiq.cloudbreak.service.notification;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.repository.SubscriptionRepository;

@Service
public class HttpNotificationSender implements NotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotificationSender.class);

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    @Qualifier("autoSSLAcceptorRestTemplate")
    private RestOperations restTemplate;

    @Override
    public void send(Notification notification) {
        List<Subscription> subscriptions = (List<Subscription>) subscriptionRepository.findAll();
        for (Subscription subscription : subscriptions) {
            String endpoint = subscription.getEndpoint();
            try {
                restTemplate.postForLocation(endpoint, notification);
            } catch (RestClientException ex) {
                LOGGER.info("Could not send notification to the specified endpoint: '{}' Cause: {}", endpoint, ex.getMessage());
            }
        }
    }
}
