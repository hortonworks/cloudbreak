package com.sequenceiq.periscope.subscription;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.Subscription;
import com.sequenceiq.periscope.repository.SubscriptionRepository;

@Service
public class SubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

    @Inject
    private SubscriptionRepository subscriptionRepository;

    public void subscribe(Subscription subscription) {
        subscriptionRepository.findByClientId(subscription.getClientId()).ifPresentOrElse(
                dbSubscription -> {
                    if (!dbSubscription.getEndpoint().equals(subscription.getEndpoint())) {
                        dbSubscription.setEndpoint(subscription.getEndpoint());
                        subscriptionRepository.save(dbSubscription);
                    }
                },
                () -> {
                    subscriptionRepository.save(subscription);
                });

        LOGGER.info("Subscription updated for this client with the endpoint [client: '{}', endpoint: '{}']",
                subscription.getClientId(), subscription.getEndpoint());
    }
}
