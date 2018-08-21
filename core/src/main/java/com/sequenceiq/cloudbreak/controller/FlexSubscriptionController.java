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
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionRequestToFlexSubscriptionConverter;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionToJsonConverter;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@Transactional(TxType.NEVER)
public class FlexSubscriptionController implements FlexSubscriptionEndpoint {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private FlexSubscriptionRequestToFlexSubscriptionConverter toFlexSubscriptionConverter;

    @Inject
    private FlexSubscriptionToJsonConverter toJsonConverter;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public FlexSubscriptionResponse get(Long id) {
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public FlexSubscriptionResponse delete(Long id) {
        FlexSubscription flexSubscription = flexSubscriptionService.delete(id);
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public FlexSubscriptionResponse deletePublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        FlexSubscription flexSubscription = flexSubscriptionService.deleteByNameFromOrganization(name, organization.getId());
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public FlexSubscriptionResponse deletePrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        FlexSubscription flexSubscription = flexSubscriptionService.deleteByNameFromOrganization(name, organization.getId());
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public FlexSubscriptionResponse postPublic(FlexSubscriptionRequest flexSubscription) {
        return createFlexSubscription(flexSubscription);
    }

    @Override
    public List<FlexSubscriptionResponse> getPublics() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUserAndOrganization(user, organization.getId());
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        FlexSubscription subscription = flexSubscriptionService.getByNameForOrganization(name, organization);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void setDefaultInAccount(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        flexSubscriptionService.setDefaultFlexSubscription(name, user, organization);
    }

    @Override
    public void setUsedForControllerInAccount(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        flexSubscriptionService.setUsedForControllerFlexSubscription(name, user, organization);
    }

    @Override
    public FlexSubscriptionResponse postPrivate(FlexSubscriptionRequest flexSubscription) {
        return createFlexSubscription(flexSubscription);
    }

    @Override
    public List<FlexSubscriptionResponse> getPrivates() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUserAndOrganization(user, organization.getId());
        return toJsonConverter.convert(subscriptions);
    }

    @Override
    public FlexSubscriptionResponse getPrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        FlexSubscription subscription = flexSubscriptionService.getByNameForOrganization(name, organization);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public void setDefaultInAccount(Long id) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        flexSubscriptionService.setDefaultFlexSubscription(flexSubscription.getName(), user, organization);
    }

    @Override
    public void setUsedForControllerInAccount(Long id) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        FlexSubscription flexSubscription = flexSubscriptionService.get(id);
        flexSubscriptionService.setUsedForControllerFlexSubscription(flexSubscription.getName(), user, organization);
    }

    private FlexSubscriptionResponse createFlexSubscription(FlexSubscriptionRequest json) {
        FlexSubscription subscription = toFlexSubscriptionConverter.convert(json);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        subscription = flexSubscriptionService.create(subscription, organization, user);
        return toJsonConverter.convert(subscription);
    }
}
