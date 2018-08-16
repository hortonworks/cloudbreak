package com.sequenceiq.cloudbreak.controller;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.FlexSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionRequestToFlexSubscriptionConverter;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionToJsonConverter;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Controller
@Transactional(TxType.NEVER)
public class FlexSubscriptionController implements FlexSubscriptionEndpoint {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private FlexSubscriptionRequestToFlexSubscriptionConverter toFlexSubscriptionConverter;

    @Inject
    private FlexSubscriptionToJsonConverter toJsonConverter;

    @Inject
    private OrganizationService organizationService;

    @Override
    public FlexSubscriptionResponse get(Long id) {
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public void delete(Long id) {
        flexSubscriptionService.delete(id);
    }

    @Override
    public void deletePublic(String name) {
        flexSubscriptionService.deleteByNameFromDefaultOrganization(name);
    }

    @Override
    public void deletePrivate(String name) {
        flexSubscriptionService.deleteByNameFromDefaultOrganization(name);
    }

    @Override
    public FlexSubscriptionResponse postPublic(FlexSubscriptionRequest flexSubscription) {
        return createFlexSubscription(flexSubscription);
    }

    @Override
    public List<FlexSubscriptionResponse> getPublics() {
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUsersDefaultOrganization();
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPublic(String name) {
        FlexSubscription subscription = flexSubscriptionService.getByNameFromUsersDefaultOrganization(name);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void setDefaultInAccount(String name) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        flexSubscriptionService.setDefaultFlexSubscription(name, identityUser);
    }

    @Override
    public void setUsedForControllerInAccount(String name) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        flexSubscriptionService.setUsedForControllerFlexSubscription(name, identityUser);
    }

    @Override
    public FlexSubscriptionResponse postPrivate(FlexSubscriptionRequest flexSubscription) {
        return createFlexSubscription(flexSubscription);
    }

    @Override
    public List<FlexSubscriptionResponse> getPrivates() {
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUsersDefaultOrganization();
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPrivate(String name) {
        FlexSubscription subscription = flexSubscriptionService.getByNameFromUsersDefaultOrganization(name);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void setDefaultInAccount(Long id) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        flexSubscriptionService.setDefaultFlexSubscription(flexSubscription.getName(), identityUser);
    }

    @Override
    public void setUsedForControllerInAccount(Long id) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        flexSubscriptionService.setUsedForControllerFlexSubscription(flexSubscription.getName(), identityUser);
    }

    private FlexSubscriptionResponse createFlexSubscription(FlexSubscriptionRequest json) {
        FlexSubscription subscription = toFlexSubscriptionConverter.convert(json);
        Organization defaultOrganizationForCurrentUser = organizationService.getDefaultOrganizationForCurrentUser();
        subscription = flexSubscriptionService.create(subscription, defaultOrganizationForCurrentUser);
        return toJsonConverter.convert(subscription);
    }
}
