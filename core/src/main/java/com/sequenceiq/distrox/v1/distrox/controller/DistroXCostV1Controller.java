package com.sequenceiq.distrox.v1.distrox.controller;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.service.cost.ClusterCostService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXCostV1Endpoint;

@Controller
public class DistroXCostV1Controller implements DistroXCostV1Endpoint {

    @Inject
    private ClusterCostService clusterCostService;

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public RealTimeCostResponse list(@ResourceCrnList List<String> datahubCrns) {
        return new RealTimeCostResponse(clusterCostService.getCosts(datahubCrns, List.of()));
    }
}
