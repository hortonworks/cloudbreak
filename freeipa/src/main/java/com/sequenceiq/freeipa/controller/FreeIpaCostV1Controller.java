package com.sequenceiq.freeipa.controller;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.freeipa.api.v1.cost.FreeIpaCostV1Endpoint;
import com.sequenceiq.freeipa.cost.FreeIpaCostService;
import com.sequenceiq.freeipa.entity.Stack;

@Controller
@AccountEntityType(Stack.class)
public class FreeIpaCostV1Controller implements FreeIpaCostV1Endpoint {

    @Inject
    private FreeIpaCostService freeIpaCostService;

    @Override
    @InternalOnly
    public RealTimeCostResponse list(List<String> environmentCrns, @InitiatorUserCrn String initiatorUserCrn) {
        return new RealTimeCostResponse(freeIpaCostService.getCosts(environmentCrns));
    }
}
