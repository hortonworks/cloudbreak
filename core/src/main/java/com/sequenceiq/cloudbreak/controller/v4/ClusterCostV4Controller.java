package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.ClusterCostV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.authorization.StackFiltering;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.service.cost.ClusterCostService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class ClusterCostV4Controller implements ClusterCostV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCostV4Controller.class);

    @Inject
    private StackFiltering stackFiltering;

    @Inject
    private ClusterCostService clusterCostService;

    @Override
    @FilterListBasedOnPermissions
    public RealTimeCostResponse list() {
        StackViewV4Responses responses = stackFiltering.filterDataHubs(DESCRIBE_DATAHUB, null, null);
        return new RealTimeCostResponse(clusterCostService.getCosts(responses));
    }
}
