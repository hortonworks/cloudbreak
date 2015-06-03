package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.SubscriptionRequest;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.subscription.SubscriptionService;

@Controller
public class SubscriptionController {

    @Inject
    private SubscriptionService subscriptionService;

    @RequestMapping(value = "subscriptions", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> subscribeClient(@ModelAttribute("user") CbUser user, @RequestBody @Valid SubscriptionRequest subscriptionRequest,
            @AuthenticationPrincipal String client) {
        MDCBuilder.buildMdcContext(user);
        Subscription subscription = new Subscription(client, subscriptionRequest.getEndpointUrl());
        return new ResponseEntity<IdJson>(new IdJson(subscriptionService.subscribe(subscription)), HttpStatus.CREATED);
    }
}
