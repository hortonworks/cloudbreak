package com.sequenceiq.environment.environment.v1.cost;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.cost.EnvironmentRealTimeCostResponse;
import com.sequenceiq.cloudbreak.cost.CostCalculationNotEnabledException;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCostController.class);

    @Inject
    private EnvironmentFiltering environmentFiltering;

    @Inject
    private EnvironmentCostService environmentCostService;

    @Inject
    private EntitlementService entitlementService;

    @Override
    @FilterListBasedOnPermissions
    public EnvironmentRealTimeCostResponse list() {
        checkIfCostCalculationIsEnabled();
        List<EnvironmentDto> environmentDtos = environmentFiltering.filterEnvironments(DESCRIBE_ENVIRONMENT);
        return new EnvironmentRealTimeCostResponse(environmentCostService.getCosts(environmentDtos));
    }

    private void checkIfCostCalculationIsEnabled() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        boolean usdCalculationEnabled = entitlementService.isUsdCostCalculationEnabled(accountId);
        boolean co2CalculationEnabled = entitlementService.isCO2CalculationEnabled(accountId);

        if (!usdCalculationEnabled && !co2CalculationEnabled) {
            LOGGER.info("Both USD cost calculation and CO2 cost calculation features are disable!");
            throw new CostCalculationNotEnabledException("Cost calculation feature is not enabled!");
        }

        if (!usdCalculationEnabled) {
            LOGGER.info("USD cost calculation feature is disabled!");
        }
        if (!co2CalculationEnabled) {
            LOGGER.info("CO2 cost calculation feature is disabled");
        }
    }
}
