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
import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
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

    @Inject
    private Map<CloudPlatform, PricingCache> pricingCacheMap;

    public Map<String, RealTimeCost> getCosts(List<String> environmentCrns) {
        errorIfCostCalculationFeatureIsNotEnabled();
        Map<String, RealTimeCost> realTimeCosts = new HashMap<>();

        List<Stack> stacks = stackService.getByEnvironmentCrnsAndCloudPlatforms(environmentCrns, pricingCacheMap.keySet());
        for (Stack stack : stacks) {
            ClusterCostDto clusterCost = instanceTypeCollectorService.getAllInstanceTypes(stack);
            RealTimeCost realTimeCost = new RealTimeCost();
            realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
            realTimeCost.setResourceCrn(stack.getResourceCrn());
            realTimeCost.setType("FREEIPA");
            realTimeCost.setResourceName(stack.getResourceName());
            realTimeCost.setHourlyProviderUsd(usdCalculatorService.calculateProviderCost(clusterCost));
            realTimeCost.setHourlyClouderaUsd(usdCalculatorService.calculateClouderaCost(clusterCost, realTimeCost.getType()));
            realTimeCosts.put(stack.getResourceCrn(), realTimeCost);
        }

        return realTimeCosts;
    }

    private void errorIfCostCalculationFeatureIsNotEnabled() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (!entitlementService.isUsdCostCalculationEnabled(accountId)) {
            throw new CostCalculationNotEnabledException("Cost calculation features are not enabled!");
        }
    }
}
