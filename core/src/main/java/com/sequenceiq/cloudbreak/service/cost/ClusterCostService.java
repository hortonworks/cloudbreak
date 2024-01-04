package com.sequenceiq.cloudbreak.service.cost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
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
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterCostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCostService.class);

    @Inject
    private UsdCalculatorService usdCalculatorService;

    @Inject
    private CO2CostCalculatorService co2CostCalculatorService;

    @Inject
    private InstanceTypeCollectorService instanceTypeCollectorService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private Map<CloudPlatform, PricingCache> pricingCacheMap;

    @Inject
    private Map<CloudPlatform, CO2EmissionFactorService> co2EmissionFactorServiceMap;

    public Map<String, RealTimeCost> getCosts(List<String> clusterCrns, List<String> environmentCrns) {
        errorIfCostCalculationFeatureIsNotEnabled();
        Map<String, RealTimeCost> realTimeCosts = new HashMap<>();

        List<StackView> stacksByCrns = Lists.newArrayList();
        List<StackView> stackByEnvs = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(clusterCrns)) {
            stacksByCrns = stackDtoService.findNotTerminatedByResourceCrnsAndCloudPlatforms(clusterCrns, pricingCacheMap.keySet());
        }

        if (CollectionUtils.isNotEmpty(environmentCrns)) {
            stackByEnvs = stackDtoService.findNotTerminatedByEnvironmentCrnsAndCloudPlatforms(environmentCrns, pricingCacheMap.keySet());
        }

        List<StackView> stacks = ListUtils.union(stacksByCrns, stackByEnvs);
        for (StackView stack : stacks) {
            Optional<ClusterCostDto> clusterCostDto = instanceTypeCollectorService.getAllInstanceTypesForCost(stack);

            if (clusterCostDto.isPresent()) {
                RealTimeCost realTimeCost = new RealTimeCost();
                realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
                realTimeCost.setResourceCrn(stack.getResourceCrn());
                realTimeCost.setType(stack.getType().name());
                realTimeCost.setResourceName(stack.getName());
                LOGGER.info("Calculating USD cost for resource {}", stack.getResourceCrn());
                realTimeCost.setHourlyProviderUsd(usdCalculatorService.calculateProviderCost(clusterCostDto.get()));
                realTimeCost.setHourlyClouderaUsd(usdCalculatorService.calculateClouderaCost(clusterCostDto.get(), realTimeCost.getType()));
                realTimeCosts.put(stack.getResourceCrn(), realTimeCost);
            }
        }
        return realTimeCosts;
    }

    public Map<String, RealTimeCO2> getCO2(List<String> clusterCrns, List<String> environmentCrns) {
        errorIfCO2CalculationFeatureIsNotEnabled();
        Map<String, RealTimeCO2> realTimeCO2Map = new HashMap<>();
        List<StackView> stacksByCrns = Lists.newArrayList();
        List<StackView> stackByEnvs = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(clusterCrns)) {
            stacksByCrns = stackDtoService.findNotTerminatedByResourceCrnsAndCloudPlatforms(clusterCrns, co2EmissionFactorServiceMap.keySet());
        }
        if (CollectionUtils.isNotEmpty(environmentCrns)) {
            stackByEnvs = stackDtoService.findNotTerminatedByEnvironmentCrnsAndCloudPlatforms(environmentCrns, co2EmissionFactorServiceMap.keySet());
        }

        List<StackView> stacks = ListUtils.union(stacksByCrns, stackByEnvs);
        for (StackView stack : stacks) {
            Optional<ClusterCO2Dto> clusterCO2Dto = instanceTypeCollectorService.getAllInstanceTypesForCO2(stack);

            if (clusterCO2Dto.isPresent()) {
                RealTimeCO2 realTimeCO2 = new RealTimeCO2();
                realTimeCO2.setEnvCrn(stack.getEnvironmentCrn());
                realTimeCO2.setResourceCrn(stack.getResourceCrn());
                realTimeCO2.setType(stack.getType().name());
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
