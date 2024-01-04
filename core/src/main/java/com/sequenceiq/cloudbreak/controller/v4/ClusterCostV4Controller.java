package com.sequenceiq.cloudbreak.controller.v4;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.ClusterCostV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.requests.ClusterCostV4Request;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.service.cost.ClusterCostService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class ClusterCostV4Controller implements ClusterCostV4Endpoint {

    @Inject
    private ClusterCostService clusterCostService;

    @Override
    @InternalOnly
    public RealTimeCostResponse list(List<String> clusterCrns, @InitiatorUserCrn String initiatorUserCrn) {
        return new RealTimeCostResponse(clusterCostService.getCosts(clusterCrns, List.of()));
    }

    @Override
    @InternalOnly
    public RealTimeCostResponse listByEnv(ClusterCostV4Request clusterCostV4Request, @InitiatorUserCrn  String initiatorUserCrn) {
        return new RealTimeCostResponse(clusterCostService.getCosts(clusterCostV4Request.getClusterCrns(), clusterCostV4Request.getEnvironmentCrns()));
    }
}
