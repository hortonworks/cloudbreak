package com.sequenceiq.cloudbreak.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.SubscriptionEndpoint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.model.IdJson;
import com.sequenceiq.cloudbreak.model.SubscriptionRequest;
import com.sequenceiq.cloudbreak.service.subscription.SubscriptionService;

@Component
public class SubscriptionController implements SubscriptionEndpoint {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    public IdJson subscribe(SubscriptionRequest subscriptionRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Subscription subscription = new Subscription(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(),
                subscriptionRequest.getEndpointUrl());
        return new IdJson(subscriptionService.subscribe(subscription));
    }
}
