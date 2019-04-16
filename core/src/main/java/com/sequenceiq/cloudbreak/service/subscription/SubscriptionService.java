package com.sequenceiq.cloudbreak.service.subscription;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.repository.SubscriptionRepository;

@Service
public class SubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

    @Inject
    private SubscriptionRepository subscriptionRepository;

    public Long subscribe(Subscription subscription) {
        List<Subscription> clientSubscriptions = subscriptionRepository.findByClientIdAndEndpoint(subscription.getClientId(), subscription.getEndpoint());
        if (!clientSubscriptions.isEmpty()) {
            LOGGER.debug("Subscription already exists for this client with the same endpoint [client: '{}', endpoint: '{}']",
                    subscription.getClientId(), subscription.getEndpoint());
            return clientSubscriptions.get(0).getId();
        }
        return subscriptionRepository.save(subscription).getId();
    }

    public Iterable<Subscription> findAll() {
        return subscriptionRepository.findAll();
    }

}
