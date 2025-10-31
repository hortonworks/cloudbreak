package com.sequenceiq.freeipa.controller;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v2.freeipa.crossrealm.TrustV2Endpoint;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2Request;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.TrustSetupService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@AccountEntityType(Stack.class)
public class TrustV2Controller implements TrustV2Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustV2Controller.class);

    @Inject
    private CrnService crnService;

    @Inject
    private TrustSetupService trustSetupService;

    @Override
    @InternalOnly
    public PrepareCrossRealmTrustResponse setup(@RequestObject PrepareCrossRealmTrustV2Request request, @InitiatorUserCrn String initiatorUserCrn) {
        String accountId = crnService.getCurrentAccountId();
        return trustSetupService.setupTrust(accountId, request);
    }
}
