package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.service.cost.ClusterCostService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXCostV1Endpoint;
import com.sequenceiq.distrox.v1.distrox.authorization.DataHubFiltering;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class DistroXCostV1Controller implements DistroXCostV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXCostV1Controller.class);

    @Inject
    private DataHubFiltering dataHubFiltering;

    @Inject
    private ClusterCostService clusterCostService;

    @Override
    @FilterListBasedOnPermissions
    public RealTimeCostResponse list() {
        StackViewV4Responses responses = dataHubFiltering.filterDataHubs(DESCRIBE_DATAHUB, null, null);
        List<RealTimeCost> realTimeCosts = clusterCostService.getCosts(responses);
        return new RealTimeCostResponse(realTimeCosts);
    }
}
