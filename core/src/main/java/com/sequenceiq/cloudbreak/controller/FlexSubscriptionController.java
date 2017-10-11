package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.FlexSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionToJsonConverter;
import com.sequenceiq.cloudbreak.converter.JsonToFlexSubscriptionConverter;
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
        flexService.delete(id);
    }

    @Override
    public void deletePublic(String name) {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        FlexSubscription subscription = flexService.findByNameInAccount(name, cbUser.getUserId(), cbUser.getAccount());
        flexService.delete(subscription);
    }

    @Override
    public void deletePrivate(String name) {
        FlexSubscription subscription = flexService.findOneByName(name);
        flexService.delete(subscription);
    }

    @Override
    public FlexSubscriptionResponse postPublic(FlexSubscriptionRequest flexSubscription) {
        return createFlexSubscription(flexSubscription, true);
    }

    @Override
    public List<FlexSubscriptionResponse> getPublics() {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        List<FlexSubscription> subscriptions = flexService.findPublicInAccountForUser(identityUser);
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPublic(String name) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        FlexSubscription subscription = flexService.findByNameInAccount(name, identityUser.getUserId(), identityUser.getAccount());
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void setDefaultInAccount(String name) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        flexService.setDefaultFlexSubscription(name, identityUser);
    }

    @Override
    public void setUsedForControllerInAccount(String name) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        flexService.setUsedForControllerFlexSubscription(name, identityUser);
    }

    @Override
    public FlexSubscriptionResponse postPrivate(FlexSubscriptionRequest flexSubscription) {
        return createFlexSubscription(flexSubscription, false);
    }

    @Override
    public List<FlexSubscriptionResponse> getPrivates() {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        List<FlexSubscription> subscriptions = flexService.findByOwner(identityUser.getUserId());
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPrivate(String name) {
        FlexSubscription subscription = flexService.findOneByName(name);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void setDefaultInAccount(Long id) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        FlexSubscription flexSubscription = flexService.findOneById(id);
        flexService.setDefaultFlexSubscription(flexSubscription.getName(), identityUser);
    }

    @Override
    public void setUsedForControllerInAccount(Long id) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        FlexSubscription flexSubscription = flexService.findOneById(id);
        flexService.setUsedForControllerFlexSubscription(flexSubscription.getName(), identityUser);
    }

    private FlexSubscriptionResponse createFlexSubscription(FlexSubscriptionRequest json, boolean publicInAccount) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        FlexSubscription subscription = toFlexSubscriptionConverter.convert(json);
        subscription.setAccount(identityUser.getAccount());
        subscription.setOwner(identityUser.getUserId());
        subscription.setPublicInAccount(publicInAccount);
        subscription = flexService.create(subscription);
        return toJsonConverter.convert(subscription);
    }
}
