package com.sequenceiq.cloudbreak.service.subscription;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.repository.SubscriptionRepository;

@Service
public class SimpleSubscriptionService implements SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Override
    public Long subscribe(Subscription subscription) {
        //check if client is already subscribed
        return subscriptionRepository.save(subscription).getId();
    }
}
