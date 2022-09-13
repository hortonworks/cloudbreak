package com.sequenceiq.environment.environment.v1.cost;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentCostV1Endpoint;
import com.sequenceiq.environment.authorization.EnvironmentFiltering;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.cost.EnvironmentCostService;

@Controller
@Transactional(Transactional.TxType.NEVER)
@AccountEntityType(Environment.class)
public class EnvironmentCostController implements EnvironmentCostV1Endpoint {

    @Inject
    private EnvironmentFiltering environmentFiltering;

    @Inject
    private EnvironmentCostService environmentCostService;

    @Override
    @FilterListBasedOnPermissions
    public RealTimeCostResponse list() {
        List<EnvironmentDto> environmentDtos = environmentFiltering.filterEnvironments(DESCRIBE_ENVIRONMENT);
        List<RealTimeCost> realTimeCosts = environmentCostService.getCosts(environmentDtos);
        return new RealTimeCostResponse(realTimeCosts);
    }
}
