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
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionRequestToFlexSubscriptionConverter;
import com.sequenceiq.cloudbreak.converter.FlexSubscriptionToJsonConverter;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;

@Controller
@Transactional(TxType.NEVER)
public class FlexSubscriptionV3Controller implements FlexSubscriptionV3Endpoint {

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private FlexSubscriptionRequestToFlexSubscriptionConverter toFlexSubscriptionConverter;

    @Inject
    private FlexSubscriptionToJsonConverter toJsonConverter;

    @Override
    public Set<FlexSubscriptionResponse> listByOrganization(Long organizationId) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        Set<FlexSubscription> subscriptions = flexSubscriptionService.findAllForUserAndOrganization(identityUser, organizationId);
        return new HashSet<>(toJsonConverter.convert(subscriptions));
    }

    @Override
    public FlexSubscriptionResponse getByNameInOrganization(Long organizationId, String name) {
        FlexSubscription subscription = flexSubscriptionService.findOneByNameAndOrganization(name, organizationId);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public FlexSubscriptionResponse createInOrganization(Long organizationId, FlexSubscriptionRequest request) {
        FlexSubscription subscription = toFlexSubscriptionConverter.convert(request);
        subscription = flexSubscriptionService.create(subscription, organizationId);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public FlexSubscriptionResponse deleteInOrganization(Long organizationId, String name) {
        FlexSubscription flexSubscription = flexSubscriptionService.deleteByNameFromOrganization(name, organizationId);
        return toJsonConverter.convert(flexSubscription);
    }

    @Override
    public void setUsedForControllerInOrganization(Long organizationId, String name) {

    }

    @Override
    public void setDefaultInOrganization(Long organizationId, String name) {

    }
}
