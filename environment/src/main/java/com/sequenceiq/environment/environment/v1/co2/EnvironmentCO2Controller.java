package com.sequenceiq.environment.environment.v1.co2;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN_LIST;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.cloudbreak.common.co2.EnvironmentRealTimeCO2;
import com.sequenceiq.cloudbreak.common.co2.EnvironmentRealTimeCO2Response;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentCO2V1Endpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRealTimeCO2Request;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.cost.EnvironmentCostService;

@Controller
@Transactional(Transactional.TxType.NEVER)
@AccountEntityType(Environment.class)
public class EnvironmentCO2Controller implements EnvironmentCO2V1Endpoint {

    @Inject
    private EnvironmentCostService environmentCostService;

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrns", type = CRN_LIST, action = DESCRIBE_ENVIRONMENT)
    public EnvironmentRealTimeCO2Response list(@RequestObject EnvironmentRealTimeCO2Request request) {
        List<String> clusterCrns = ListUtils.union(ListUtils.emptyIfNull(request.getDatalakeCrns()), ListUtils.emptyIfNull(request.getDatahubCrns()));
        Map<String, EnvironmentRealTimeCO2> co2Map = environmentCostService.getCO2(ListUtils.emptyIfNull(request.getEnvironmentCrns()), clusterCrns);
        return new EnvironmentRealTimeCO2Response(co2Map);
    }
}
