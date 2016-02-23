package com.sequenceiq.cloudbreak.service.subscription;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.repository.SubscriptionRepository;

@Service
@Transactional
public class SubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Transactional(Transactional.TxType.NEVER)
    public Long subscribe(Subscription subscription) {
        Subscription exists = null;
        List<Subscription> clientSubscriptions = subscriptionRepository.findByClientId(subscription.getClientId());
        for (Subscription s : clientSubscriptions) {
            if (s.getEndpoint().equals(subscription.getEndpoint())) {
                exists = s;
                LOGGER.info(String.format("Subscription already exists for this client with the same endpoint [client: '%s', endpoint: '%s']",
                                subscription.getClientId(),
                                subscription.getEndpoint()));
                break;
            }
        }
        return exists == null ? subscriptionRepository.save(subscription).getId() : exists.getId();
    }
}
