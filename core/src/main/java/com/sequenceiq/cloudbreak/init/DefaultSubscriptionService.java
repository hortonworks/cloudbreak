package com.sequenceiq.cloudbreak.init;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.service.subscription.SubscriptionService;

@Component
public class DefaultSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSubscriptionService.class);
    private static final String DEFAULT_CLIENT_ID = "default";

    @Value("${cb.default.subscription.address:}")
    private String defaultSubscriptionAddress;

    @Inject
    private SubscriptionService subscriptionService;

    @PostConstruct
    public void init() {
        if (!Strings.isNullOrEmpty(defaultSubscriptionAddress)) {
            LOGGER.info("Configuring default subscription %s", defaultSubscriptionAddress);
            Subscription subscription = new Subscription();
            subscription.setClientId(DEFAULT_CLIENT_ID);
            subscription.setEndpoint(defaultSubscriptionAddress);
            subscriptionService.subscribe(subscription);
        }
    }
}
