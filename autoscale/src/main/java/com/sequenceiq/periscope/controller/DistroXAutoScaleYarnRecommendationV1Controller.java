package com.sequenceiq.periscope.controller;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleYarnRecommendationV1Endpoint;
import com.sequenceiq.periscope.api.model.DistroXAutoScaleYarnRecommendationResponse;
import com.sequenceiq.periscope.service.YarnRecommendationService;

@Controller
public class DistroXAutoScaleYarnRecommendationV1Controller implements DistroXAutoScaleYarnRecommendationV1Endpoint {

    @Inject
    private YarnRecommendationService yarnRecommendationService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public DistroXAutoScaleYarnRecommendationResponse getYarnRecommendation(@ResourceCrn @TenantAwareParam String clusterCrn) throws Exception {
        DistroXAutoScaleYarnRecommendationResponse distroXAutoScaleYarnRecommendationResponse = new DistroXAutoScaleYarnRecommendationResponse();
        List<String> yarnRecommendedDecommissionHosts = yarnRecommendationService.getRecommendationFromYarn(clusterCrn);
        distroXAutoScaleYarnRecommendationResponse.setDecommissionNodeIds(yarnRecommendedDecommissionHosts);
        return distroXAutoScaleYarnRecommendationResponse;
    }
}
