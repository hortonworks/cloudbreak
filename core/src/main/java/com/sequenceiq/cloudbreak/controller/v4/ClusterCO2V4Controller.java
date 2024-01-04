package com.sequenceiq.cloudbreak.controller.v4;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.api.endpoint.v4.co2.ClusterCO2V4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.co2.requests.ClusterCO2V4Request;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.service.cost.ClusterCostService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class ClusterCO2V4Controller implements ClusterCO2V4Endpoint {

    @Inject
    private ClusterCostService clusterCostService;

    @Override
    @InternalOnly
    public RealTimeCO2Response list(List<String> clusterCrns, @InitiatorUserCrn String initiatorUserCrn) {
        return new RealTimeCO2Response(clusterCostService.getCO2(clusterCrns, List.of()));
    }

    @Override
    @InternalOnly
    public RealTimeCO2Response listByEnv(ClusterCO2V4Request clusterCO2V4Request, @InitiatorUserCrn String initiatorUserCrn) {
        return new RealTimeCO2Response(clusterCostService.getCO2(clusterCO2V4Request.getClusterCrns(), clusterCO2V4Request.getEnvironmentCrns()));
    }
}
