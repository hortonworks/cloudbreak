package com.sequenceiq.freeipa.controller;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.freeipa.api.v1.cost.FreeIpaCostV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.authorization.FreeIpaFiltering;
import com.sequenceiq.freeipa.cost.FreeIpaCostService;
import com.sequenceiq.freeipa.entity.Stack;

@Controller
@AccountEntityType(Stack.class)
public class FreeIpaCostV1Controller implements FreeIpaCostV1Endpoint {

    @Inject
    private FreeIpaFiltering freeIpaFiltering;

    @Inject
    private FreeIpaCostService freeIpaCostService;

    @Override
    @FilterListBasedOnPermissions
    public RealTimeCostResponse list() {
        List<ListFreeIpaResponse> freeIpas = freeIpaFiltering.filterFreeIpas(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
        return new RealTimeCostResponse(freeIpaCostService.getCosts(freeIpas));
    }
}
