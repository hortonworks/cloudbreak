package com.sequenceiq.cloudbreak.service.notification;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.repository.SubscriptionRepository;

@Service
public class HttpNotificationSender implements NotificationSender {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Override
    public void send(Notification notification) {
        List<Subscription> subscriptions = (List<Subscription>) subscriptionRepository.findAll();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders requestHeaders = new HttpHeaders();

        for (Subscription subscription : subscriptions) {
            restTemplate.postForLocation(subscription.getEndpoint(), notification);
        }
    }
}
