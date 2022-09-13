package com.sequenceiq.cloudbreak.service.cost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.cost.CostCalculationNotEnabledException;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.usd.UsdCalculatorService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterCostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCostService.class);

    @Inject
    private UsdCalculatorService usdCalculatorService;

    @Inject
    private InstanceTypeCollectorService instanceTypeCollectorService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private EntitlementService entitlementService;

    private boolean usdCalculationEnabled;

    public Map<String, RealTimeCost> getCosts(List<String> clusterCrns) {
        checkIfCostCalculationIsEnabled();
        errorIfCostCalculationFeatureIsNotEnabled();
        Map<String, RealTimeCost> realTimeCosts = new HashMap<>();

        List<StackView> stacks = stackDtoService.findNotTerminatedByCrns(clusterCrns);
        for (StackView stack : stacks) {
            Status stackStatus = stack.getStatus();
            if (stackStatus == Status.DELETE_COMPLETED || stackStatus == Status.DELETED_ON_PROVIDER_SIDE) {
                LOGGER.info("Stack with crn {} has DELETE_COMPLETED / DELETED_ON_PROVIDER_SIDE status, " +
                        "therefore cost calculation for this stack is skipped.", stack.getResourceCrn());
                continue;
            }

            ClusterCostDto clusterCost = instanceTypeCollectorService.getAllInstanceTypes(stack);
            RealTimeCost realTimeCost = new RealTimeCost();
            realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
            realTimeCost.setType(stack.getType().name());
            realTimeCost.setResourceName(stack.getName());

            if (usdCalculationEnabled) {
                realTimeCost.setHourlyProviderUsd(usdCalculatorService.calculateProviderCost(clusterCost));
                realTimeCost.setHourlyClouderaUsd(usdCalculatorService.calculateClouderaCost(clusterCost, realTimeCost.getType()));
            }
            realTimeCosts.put(stack.getResourceCrn(), realTimeCost);
        }
        return realTimeCosts;
    }

    private void checkIfCostCalculationIsEnabled() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        usdCalculationEnabled = entitlementService.isUsdCostCalculationEnabled(accountId);

        if (!usdCalculationEnabled) {
            LOGGER.info("USD cost calculation feature is disabled!");
        }
    }

    private void errorIfCostCalculationFeatureIsNotEnabled() {
        if (!usdCalculationEnabled) {
            throw new CostCalculationNotEnabledException("Cost calculation features are not enabled!");
        }
    }
}
