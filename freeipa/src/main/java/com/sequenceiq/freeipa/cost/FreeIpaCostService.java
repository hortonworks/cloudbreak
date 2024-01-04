package com.sequenceiq.freeipa.cost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.co2.CO2CostCalculatorService;
import com.sequenceiq.cloudbreak.co2.CO2EmissionFactorService;
import com.sequenceiq.cloudbreak.co2.model.ClusterCO2Dto;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2;
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
    private CO2CostCalculatorService co2CostCalculatorService;

    @Inject
    private FreeIpaInstanceTypeCollectorService freeIpaInstanceTypeCollectorService;

    @Inject
    private StackService stackService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private Map<CloudPlatform, PricingCache> pricingCacheMap;

    @Inject
    private Map<CloudPlatform, CO2EmissionFactorService> co2EmissionFactorServiceMap;

    public Map<String, RealTimeCost> getCosts(List<String> environmentCrns) {
        errorIfCostCalculationFeatureIsNotEnabled();
        Map<String, RealTimeCost> realTimeCosts = new HashMap<>();

        List<Stack> stacks = stackService.getByEnvironmentCrnsAndCloudPlatforms(environmentCrns, pricingCacheMap.keySet());
        for (Stack stack : stacks) {
            Optional<ClusterCostDto> clusterCostDto = freeIpaInstanceTypeCollectorService.getAllInstanceTypesForCost(stack);

            if (clusterCostDto.isPresent()) {
                RealTimeCost realTimeCost = new RealTimeCost();
                realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
                realTimeCost.setResourceCrn(stack.getResourceCrn());
                realTimeCost.setType("FREEIPA");
                realTimeCost.setResourceName(stack.getResourceName());
                LOGGER.info("Calculating USD cost for resource {}", stack.getResourceCrn());
                realTimeCost.setHourlyProviderUsd(usdCalculatorService.calculateProviderCost(clusterCostDto.get()));
                realTimeCost.setHourlyClouderaUsd(usdCalculatorService.calculateClouderaCost(clusterCostDto.get(), realTimeCost.getType()));
                realTimeCosts.put(stack.getResourceCrn(), realTimeCost);
            }
        }

        return realTimeCosts;
    }

    public Map<String, RealTimeCO2> getCO2(List<String> environmentCrns) {
        errorIfCO2CalculationFeatureIsNotEnabled();
        Map<String, RealTimeCO2> realTimeCO2Map = new HashMap<>();

        List<Stack> stacks = stackService.getByEnvironmentCrnsAndCloudPlatforms(environmentCrns, co2EmissionFactorServiceMap.keySet());
        for (Stack stack : stacks) {
            Optional<ClusterCO2Dto> clusterCO2Dto = freeIpaInstanceTypeCollectorService.getAllInstanceTypesForCO2(stack);

            if (clusterCO2Dto.isPresent()) {
                RealTimeCO2 realTimeCO2 = new RealTimeCO2();
                realTimeCO2.setEnvCrn(stack.getEnvironmentCrn());
                realTimeCO2.setResourceCrn(stack.getResourceCrn());
                realTimeCO2.setType("FREEIPA");
                realTimeCO2.setResourceName(stack.getResourceName());
                LOGGER.info("Calculating CO2 cost for resource {}", stack.getResourceCrn());
                realTimeCO2.setHourlyCO2InGrams(co2CostCalculatorService.calculateCO2InGrams(clusterCO2Dto.get()));
                realTimeCO2Map.put(stack.getResourceCrn(), realTimeCO2);
            }
        }
        return realTimeCO2Map;
    }

    private void errorIfCostCalculationFeatureIsNotEnabled() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (!entitlementService.isUsdCostCalculationEnabled(accountId)) {
            throw new CostCalculationNotEnabledException("Cost calculation features are not enabled!");
        }
    }

    private void errorIfCO2CalculationFeatureIsNotEnabled() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (!entitlementService.isCO2CalculationEnabled(accountId)) {
            throw new CostCalculationNotEnabledException("CO2 calculation feature is not enabled!");
        }
    }
}
