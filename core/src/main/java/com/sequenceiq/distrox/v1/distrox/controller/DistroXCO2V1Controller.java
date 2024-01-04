package com.sequenceiq.distrox.v1.distrox.controller;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.service.cost.ClusterCostService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXCO2V1Endpoint;

@Controller
public class DistroXCO2V1Controller implements DistroXCO2V1Endpoint {

    @Inject
    private ClusterCostService clusterCostService;

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public RealTimeCO2Response list(@ResourceCrnList List<String> datahubCrns) {
        return new RealTimeCO2Response(clusterCostService.getCO2(datahubCrns, List.of()));
    }
}
