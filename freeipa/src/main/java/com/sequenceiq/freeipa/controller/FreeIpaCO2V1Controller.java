package com.sequenceiq.freeipa.controller;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.freeipa.api.v1.co2.FreeIpaCO2V1Endpoint;
import com.sequenceiq.freeipa.cost.FreeIpaCostService;
import com.sequenceiq.freeipa.entity.Stack;

@Controller
@AccountEntityType(Stack.class)
public class FreeIpaCO2V1Controller implements FreeIpaCO2V1Endpoint {

    @Inject
    private FreeIpaCostService freeIpaCostService;

    @Override
    @InternalOnly
    public RealTimeCO2Response list(List<String> environmentCrns, @InitiatorUserCrn String initiatorUserCrn) {
        return new RealTimeCO2Response(freeIpaCostService.getCO2(environmentCrns));
    }
}
