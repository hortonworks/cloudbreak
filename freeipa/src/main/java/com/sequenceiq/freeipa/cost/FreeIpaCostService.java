package com.sequenceiq.freeipa.cost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.cost.CostCalculationNotEnabledException;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.usd.UsdCalculatorService;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaCostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCostService.class);

    @Inject
    private UsdCalculatorService usdCalculatorService;

    @Inject
    private FreeIpaInstanceTypeCollectorService instanceTypeCollectorService;

    @Inject
    private StackService stackService;

    @Inject
    private EntitlementService entitlementService;

    private boolean usdCalculationEnabled;

    public Map<String, RealTimeCost> getCosts(List<String> environmentCrns) {
        checkIfCostCalculationIsEnabled();
        errorIfCostCalculationFeatureIsNotEnabled();
        Map<String, RealTimeCost> realTimeCosts = new HashMap<>();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        List<Stack> stacks = stackService.getMultipleDistinctByEnvironmentCrnsAndAccountIdWithList(environmentCrns, accountId);
        for (Stack stack : stacks) {
            ClusterCostDto clusterCost = instanceTypeCollectorService.getAllInstanceTypes(stack);
            RealTimeCost realTimeCost = new RealTimeCost();
            realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
            realTimeCost.setType("FREEIPA");
            realTimeCost.setResourceName(stack.getResourceName());

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
