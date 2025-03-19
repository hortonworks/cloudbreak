package com.sequenceiq.cloudbreak.service.autoscale;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleYarnRecommendationV1Endpoint;
import com.sequenceiq.periscope.api.model.DistroXAutoScaleYarnRecommendationResponse;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterServerCertUpdateRequest;

@Service
public class PeriscopeClientService {

    @Inject
    private DistroXAutoScaleYarnRecommendationV1Endpoint distroXAutoScaleYarnRecommendationV1Endpoint;

    @Inject
    private DistroXAutoScaleClusterV1Endpoint distroXAutoScaleClusterV1Endpoint;

    public List<String> getYarnRecommendedInstanceIds(String resourceCrn) throws Exception {
        DistroXAutoScaleYarnRecommendationResponse distroXAutoScaleYarnRecommendationResponse =
                distroXAutoScaleYarnRecommendationV1Endpoint.getYarnRecommendation(resourceCrn);
        return distroXAutoScaleYarnRecommendationResponse.getDecommissionNodeIds();
    }

    public void updateServerCertificateInPeriscope(String resourceCrn, String newServerCert) {
        DistroXAutoscaleClusterServerCertUpdateRequest request = new DistroXAutoscaleClusterServerCertUpdateRequest();
        request.setCrn(resourceCrn);
        request.setNewServerCert(newServerCert);
        ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> distroXAutoScaleClusterV1Endpoint.updateServerCertificate(request));
    }
}
