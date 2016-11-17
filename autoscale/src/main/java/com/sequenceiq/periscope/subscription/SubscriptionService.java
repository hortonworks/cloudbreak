package com.sequenceiq.periscope.subscription;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.Subscription;
import com.sequenceiq.periscope.repository.SubscriptionRepository;


@Service
@Transactional
public class SubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Transactional(Transactional.TxType.NEVER)
    public Long subscribe(Subscription subscription) {
        List<Subscription> clientSubscriptions = subscriptionRepository.findByClientIdAndEndpoint(subscription.getClientId(), subscription.getEndpoint());
        if (!clientSubscriptions.isEmpty()) {
            LOGGER.info(String.format("Subscription already exists for this client with the same endpoint [client: '%s', endpoint: '%s']",
                    subscription.getClientId(), subscription.getEndpoint()));
            return clientSubscriptions.get(0).getId();
        }
        return subscriptionRepository.save(subscription).getId();
    }
}
