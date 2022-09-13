package com.sequenceiq.environment.environment.v1.cost;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN_LIST;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.cloudbreak.common.cost.EnvironmentRealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.EnvironmentRealTimeCostResponse;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentCostV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRealTimeCostRequest;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.cost.EnvironmentCostService;

@Controller
@Transactional(Transactional.TxType.NEVER)
@AccountEntityType(Environment.class)
public class EnvironmentCostController implements EnvironmentCostV1Endpoint {

    @Inject
    private EnvironmentCostService environmentCostService;

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrns", type = CRN_LIST, action = DESCRIBE_ENVIRONMENT)
    public EnvironmentRealTimeCostResponse list(@RequestObject EnvironmentRealTimeCostRequest request) {
        Map<String, EnvironmentRealTimeCost> costs = environmentCostService.getCosts(request.getEnvironmentCrns(),
                Stream.concat(request.getDatalakeCrns().stream(), request.getDatahubCrns().stream()).collect(Collectors.toList()));
        return new EnvironmentRealTimeCostResponse(costs);
    }
}
