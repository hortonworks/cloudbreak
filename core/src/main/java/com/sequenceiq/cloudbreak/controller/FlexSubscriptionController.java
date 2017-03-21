package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.FlexSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionToJsonConverter;
import com.sequenceiq.cloudbreak.converter.JsonToFlexSubscriptionConverter;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;

@Controller
public class FlexSubscriptionController implements FlexSubscriptionEndpoint {

    @Inject
    private FlexSubscriptionService flexService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private JsonToFlexSubscriptionConverter toFlexSubscriptionConverter;

    @Inject
    private FlexSubscriptionToJsonConverter toJsonConverter;

    @Override
    public FlexSubscriptionResponse get(Long id) {
        FlexSubscription flexSubscription = flexService.findOneById(id);
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public void delete(Long id) {
        CbUser cbUser = authenticatedUserService.getCbUser();
        flexService.delete(id, cbUser);
    }

    @Override
    public FlexSubscriptionResponse postPublic(FlexSubscriptionRequest flexSubscription) {
        return createFlexSubscription(flexSubscription, true);
    }

    @Override
    public List<FlexSubscriptionResponse> getPublics() {
        CbUser cbUser = authenticatedUserService.getCbUser();
        List<FlexSubscription> subscriptions = flexService.findPublicInAccountForUser(cbUser);
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPublic(String name) {
        CbUser cbUser = authenticatedUserService.getCbUser();
        FlexSubscription subscription = flexService.findByNameInAccount(name, cbUser.getUserId(), cbUser.getAccount());
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void deletePublic(String name) {
        CbUser cbUser = authenticatedUserService.getCbUser();
        FlexSubscription subscription = flexService.findByNameInAccount(name, cbUser.getUserId(), cbUser.getAccount());
        flexService.delete(subscription, cbUser);
    }

    @Override
    public FlexSubscriptionResponse postPrivate(FlexSubscriptionRequest flexSubscription) {
        return createFlexSubscription(flexSubscription, false);
    }

    @Override
    public List<FlexSubscriptionResponse> getPrivates() {
        CbUser cbUser = authenticatedUserService.getCbUser();
        List<FlexSubscription> subscriptions = flexService.findByOwner(cbUser.getUserId());
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPrivate(String name) {
        FlexSubscription subscription = flexService.findOneByName(name);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void deletePrivate(String name) {
        CbUser cbUser = authenticatedUserService.getCbUser();
        FlexSubscription subscription = flexService.findOneByName(name);
        flexService.delete(subscription, cbUser);
    }

    private FlexSubscriptionResponse createFlexSubscription(FlexSubscriptionRequest json, boolean publicInAccount) {
        CbUser cbUser = authenticatedUserService.getCbUser();
        FlexSubscription subscription = toFlexSubscriptionConverter.convert(json);
        subscription.setAccount(cbUser.getAccount());
        subscription.setOwner(cbUser.getUserId());
        subscription.setPublicInAccount(publicInAccount);
        subscription = flexService.create(subscription);
        return toJsonConverter.convert(subscription);
    }
}
