package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.FlexSubscriptionV3Endpoint;
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
public class FlexSubscriptionV3Controller implements FlexSubscriptionV3Endpoint {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private FlexSubscriptionRequestToFlexSubscriptionConverter toFlexSubscriptionConverter;

    @Inject
    private FlexSubscriptionToJsonConverter toJsonConverter;

    @Inject
    private OrganizationService organizationService;

    @Override
    public Set<FlexSubscriptionResponse> listByOrganization(Long organizationId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUserAndOrganization(user, organizationId);
        return new HashSet<>(toJsonConverter.convert(subscriptions));
    }

    @Override
    public FlexSubscriptionResponse getByNameInOrganization(Long organizationId, String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        FlexSubscription subscription = flexSubscriptionService.findOneByNameAndOrganization(name, organizationId, user);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public FlexSubscriptionResponse createInOrganization(Long organizationId, FlexSubscriptionRequest request) {
        FlexSubscription subscription = toFlexSubscriptionConverter.convert(request);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        subscription = flexSubscriptionService.create(subscription, organizationId, user);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public FlexSubscriptionResponse deleteInOrganization(Long organizationId, String name) {
        FlexSubscription flexSubscription = flexSubscriptionService.deleteByNameFromOrganization(name, organizationId);
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public void setUsedForControllerInOrganization(Long organizationId, String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(organizationId, user);
        flexSubscriptionService.setUsedForControllerFlexSubscription(name, user, organization);
    }

    @Override
    public void setDefaultInOrganization(Long organizationId, String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(organizationId, user);
        flexSubscriptionService.setDefaultFlexSubscription(name, user, organization);
    }
}
