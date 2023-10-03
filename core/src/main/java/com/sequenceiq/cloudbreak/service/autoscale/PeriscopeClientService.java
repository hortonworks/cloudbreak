package com.sequenceiq.cloudbreak.service.autoscale;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleYarnRecommendationV1Endpoint;
import com.sequenceiq.periscope.api.model.DistroXAutoScaleYarnRecommendationResponse;

@Service
public class PeriscopeClientService {

    @Inject
    private DistroXAutoScaleYarnRecommendationV1Endpoint distroXAutoScaleYarnRecommendationV1Endpoint;

    public List<String> getYarnRecommendedInstanceIds(String resourceCrn) throws Exception {
        DistroXAutoScaleYarnRecommendationResponse distroXAutoScaleYarnRecommendationResponse =
                distroXAutoScaleYarnRecommendationV1Endpoint.getYarnRecommendation(resourceCrn);
        return distroXAutoScaleYarnRecommendationResponse.getDecommissionNodeIds();
    }
}
