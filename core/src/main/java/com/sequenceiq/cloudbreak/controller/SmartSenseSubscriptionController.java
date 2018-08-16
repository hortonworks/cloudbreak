package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.SmartSenseSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;

@Component
@Transactional(TxType.NEVER)
public class SmartSenseSubscriptionController implements SmartSenseSubscriptionEndpoint {

    @Inject
    private SmartSenseSubscriptionService smartSenseSubService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter toJsonConverter;

    @Override
    public SmartSenseSubscriptionJson get() {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        SmartSenseSubscription subscription = smartSenseSubService.getDefaultForUser(cbUser);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public SmartSenseSubscriptionJson get(Long id) {
        SmartSenseSubscription subscription = smartSenseSubService.findById(id);
        return toJsonConverter.convert(subscription);
    }

}
