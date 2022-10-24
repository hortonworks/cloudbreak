package com.sequenceiq.cloudbreak.service.cost;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.cost.co2.CarbonCalculatorService;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.usd.UsdCalculatorService;

@Service
public class ClusterCostService {

    @Inject
    private CarbonCalculatorService carbonCalculatorService;

    @Inject
    private UsdCalculatorService usdCalculatorService;

    @Inject
    private InstanceTypeCollectorService instanceTypeCollectorService;

    @Inject
    private EntitlementService entitlementService;

    public Map<String, RealTimeCost> getCosts(StackViewV4Responses responses) {
        Map<String, RealTimeCost> realTimeCosts = new HashMap<>();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        boolean usdCalculationEnabled = entitlementService.isUsdCostCalculationEnabled(accountId);
        boolean co2CalculationEnabled = entitlementService.isCO2CalculationEnabled(accountId);

        if (usdCalculationEnabled || co2CalculationEnabled) {
            for (StackViewV4Response stack : responses.getResponses()) {
                ClusterCostDto clusterCost = instanceTypeCollectorService.getAllInstanceTypesByCrn(stack.getCrn());
                RealTimeCost realTimeCost = new RealTimeCost();
                realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
                realTimeCost.setType(stack.getType().name());
                realTimeCost.setResourceName(stack.getName());

                if (usdCalculationEnabled) {
                    realTimeCost.setHourlyProviderUsd(usdCalculatorService.calculateProviderCost(clusterCost));
                    realTimeCost.setHourlyClouderaUsd(usdCalculatorService.calculateClouderaCost(clusterCost, realTimeCost.getType()));
                }
                if (co2CalculationEnabled) {
                    realTimeCost.setHourlyCO2(carbonCalculatorService.getHourlyCarbonFootPrintByCrn(clusterCost));
                    realTimeCost.setHourlyEnergykWh(carbonCalculatorService.getHourlyEnergyConsumptionkWhByCrn(clusterCost));
                }

                realTimeCosts.put(stack.getCrn(), realTimeCost);
            }
        }
        return realTimeCosts;
    }

}
