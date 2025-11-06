package com.sequenceiq.freeipa.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaInternalV1Endpoint;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.FreeIpaUpgradeDefaultOutboundService;

@Controller
@AccountEntityType(Stack.class)
public class FreeIpaInternalV1Controller implements FreeIpaInternalV1Endpoint {

    @Inject
    private FreeIpaUpgradeDefaultOutboundService upgradeDefaultOutboundService;

    @Override
    @InternalOnly
    public OutboundType getOutboundType(@ResourceCrn String environmentCrn) {
        String accountId = Crn.safeFromString(environmentCrn).getAccountId();
        return upgradeDefaultOutboundService.getCurrentDefaultOutbound(environmentCrn, accountId);
    }
}
