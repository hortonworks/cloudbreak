package com.sequenceiq.freeipa.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.freeipa.api.v1.cost.FreeIpaCostV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.authorization.FreeIpaFiltering;
import com.sequenceiq.freeipa.entity.Stack;

@Controller
@AccountEntityType(Stack.class)
public class FreeIpaCostV1Controller implements FreeIpaCostV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCostV1Controller.class);

    private static final double MAGIC_PROVIDER_COST = 0.0;

    private static final double MAGIC_CLOUDERA_COST = 0.0;

    private static final double MAGIC_CO2 = 0.0;

    @Inject
    private FreeIpaFiltering freeIpaFiltering;

    @Override
    @FilterListBasedOnPermissions
    public RealTimeCostResponse list() {
        Map<String, RealTimeCost> realTimeCosts = new HashMap<>();

        List<ListFreeIpaResponse> freeIpas = freeIpaFiltering.filterFreeIpas(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
        for (ListFreeIpaResponse stack : freeIpas) {
            RealTimeCost realTimeCost = new RealTimeCost();
            realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
            realTimeCost.setHourlyProviderUsd(MAGIC_PROVIDER_COST);
            realTimeCost.setHourlyClouderaUsd(MAGIC_CLOUDERA_COST);
            realTimeCost.setHourlyCO2(MAGIC_CO2);
            realTimeCosts.put(stack.getCrn(), realTimeCost);
        }

        return new RealTimeCostResponse(realTimeCosts);
    }
}
