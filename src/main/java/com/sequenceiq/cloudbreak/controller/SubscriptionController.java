package com.sequenceiq.cloudbreak.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.SubscribeRequest;
import com.sequenceiq.cloudbreak.domain.Subscription;
import com.sequenceiq.cloudbreak.service.subscription.SubscriptionService;

@Controller
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @RequestMapping(value = "subscription", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> subscribeClient(@RequestBody @Valid SubscribeRequest subscribeRequest, @AuthenticationPrincipal String client) {
        Subscription subscription = new Subscription(client, subscribeRequest.getEndpointUrl());
        return new ResponseEntity<IdJson>(new IdJson(subscriptionService.subscribe(subscription)), HttpStatus.OK);
    }
}
