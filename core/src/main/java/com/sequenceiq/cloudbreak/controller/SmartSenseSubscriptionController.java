package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.SmartSenseSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;

@Controller
@Transactional(TxType.NEVER)
public class SmartSenseSubscriptionController implements SmartSenseSubscriptionEndpoint {

    @Inject
    private SmartSenseSubscriptionService smartSenseSubService;

    @Inject
    private SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter toJsonConverter;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public SmartSenseSubscriptionJson get() {
        IdentityUser cbUser = restRequestThreadLocalService.getIdentityUser();
        SmartSenseSubscription subscription = smartSenseSubService.getDefaultForUser(cbUser);
        return toJsonConverter.convert(subscription);
    }

    @Override
    public SmartSenseSubscriptionJson get(Long id) {
        SmartSenseSubscription subscription = smartSenseSubService.findById(id);
        return toJsonConverter.convert(subscription);
    }

}
