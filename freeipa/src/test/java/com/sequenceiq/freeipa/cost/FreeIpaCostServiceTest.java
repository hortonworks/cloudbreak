package com.sequenceiq.freeipa.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.co2.CO2CostCalculatorService;
import com.sequenceiq.cloudbreak.co2.CO2EmissionFactorService;
import com.sequenceiq.cloudbreak.co2.model.ClusterCO2Dto;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.usd.UsdCalculatorService;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaCostServiceTest {

    @Mock
    private UsdCalculatorService usdCalculatorService;

    @Mock
    private CO2CostCalculatorService co2CostCalculatorService;

    @Mock
    private FreeIpaInstanceTypeCollectorService instanceTypeCollectorService;

    @Mock
    private StackService stackService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private Map<CloudPlatform, PricingCache> pricingCacheMap;

    @Mock
    private Map<CloudPlatform, CO2EmissionFactorService> co2EmissionFactorServiceMap;

    @InjectMocks
    private FreeIpaCostService underTest;

    @Test
    void getCosts() {
        when(entitlementService.isUsdCostCalculationEnabled(any())).thenReturn(true);
        when(stackService.getByEnvironmentCrnsAndCloudPlatforms(any(), any())).thenReturn(List.of(getStack()));
        when(usdCalculatorService.calculateProviderCost(any())).thenReturn(0.5);
        when(usdCalculatorService.calculateClouderaCost(any(), eq("FREEIPA"))).thenReturn(0.5);
        when(instanceTypeCollectorService.getAllInstanceTypesForCost(any())).thenReturn(Optional.of(new ClusterCostDto()));

        ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> {
            Map<String, RealTimeCost> costs = underTest.getCosts(List.of("ENVIRONMENT_CRN"));

            assertEquals(1, costs.size());
            RealTimeCost realTimeCost = costs.get("RESOURCE_CRN");
            assertEquals(0.5, realTimeCost.getHourlyProviderUsd());
            assertEquals(0.5, realTimeCost.getHourlyClouderaUsd());
        });
    }

    @Test
    void getCO2() {
        when(entitlementService.isCO2CalculationEnabled(any())).thenReturn(true);
        when(stackService.getByEnvironmentCrnsAndCloudPlatforms(any(), any())).thenReturn(List.of(getStack()));
        when(co2CostCalculatorService.calculateCO2InGrams(any())).thenReturn(10.0);
        when(instanceTypeCollectorService.getAllInstanceTypesForCO2(any())).thenReturn(Optional.of(new ClusterCO2Dto()));

        ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> {
            Map<String, RealTimeCO2> co2Map = underTest.getCO2(List.of("ENVIRONMENT_CRN"));

            assertEquals(1, co2Map.size());
            RealTimeCO2 realTimeCO2 = co2Map.get("RESOURCE_CRN");
            assertEquals(10.0, realTimeCO2.getHourlyCO2InGrams());
        });
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("ENVIRONMENT_CRN");
        stack.setName("RESOURCE_NAME");
        stack.setResourceCrn("RESOURCE_CRN");
        stack.setCloudPlatform("AWS");
        return stack;
    }
}
