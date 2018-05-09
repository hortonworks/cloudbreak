package com.sequenceiq.cloudbreak.controller;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.SubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.SubscriptionRequest;
import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.service.subscription.SubscriptionService;

@Component
@Transactional(TxType.NEVER)
public class SubscriptionController implements SubscriptionEndpoint {

    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    public IdJson subscribe(SubscriptionRequest subscriptionRequest) {
        Subscription subscription = new Subscription(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(),
                subscriptionRequest.getEndpointUrl());
        return new IdJson(subscriptionService.subscribe(subscription));
    }
}
