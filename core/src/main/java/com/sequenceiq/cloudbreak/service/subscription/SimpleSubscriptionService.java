package com.sequenceiq.cloudbreak.service.subscription;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.repository.SubscriptionRepository;

@Service
public class SimpleSubscriptionService implements SubscriptionService {

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Override
    public Long subscribe(Subscription subscription) {
        List<Subscription> clientSubscriptions = subscriptionRepository.findByClientId(subscription.getClientId());
        for (Subscription s : clientSubscriptions) {
            if (s.getEndpoint().equals(subscription.getEndpoint())) {
                throw new SubscriptionAlreadyExistException(
                        String.format("Subscription already exists for this client with the same endpoint [client: '%s', endpoint: '%s']",
                                subscription.getClientId(),
                                subscription.getEndpoint()));
            }
        }
        return subscriptionRepository.save(subscription).getId();
    }
}
