package com.sequenceiq.freeipa.cost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.cost.co2.CarbonCalculatorService;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.usd.UsdCalculatorService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;

@Service
public class FreeIpaCostService {

    private static final double MAGIC_PROVIDER_COST = 5.3;

    private static final double MAGIC_CLOUDERA_COST = 1.3;

    @Inject
    private CarbonCalculatorService carbonCalculatorService;

    @Inject
    private UsdCalculatorService usdCalculatorService;

    @Inject
    private FreeIpaInstanceTypeCollectorService instanceTypeCollectorService;

    public Map<String, RealTimeCost> getCosts(List<ListFreeIpaResponse> responses) {
        Map<String, RealTimeCost> realTimeCosts = new HashMap<>();

        //TODO Ricsi / Laci code comes here
        for (ListFreeIpaResponse stack : responses) {
            ClusterCostDto clusterCost = instanceTypeCollectorService.getAllInstanceTypesByCrn(stack.getCrn());
            RealTimeCost realTimeCost = new RealTimeCost();
            realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
            realTimeCost.setType("FREEIPA");
            realTimeCost.setHourlyProviderUsd(usdCalculatorService.calculateProviderCost(clusterCost));
            realTimeCost.setHourlyClouderaUsd(usdCalculatorService.calculateClouderaCost(clusterCost));
            realTimeCost.setHourlyCO2(carbonCalculatorService.getHourlyCarbonFootPrintByCrn(clusterCost));
            realTimeCost.setHourlyEnergykWh(carbonCalculatorService.getHourlyEnergyConsumptionkWhByCrn(clusterCost));
            realTimeCosts.put(stack.getCrn(), realTimeCost);
        }
        return realTimeCosts;
    }

}
