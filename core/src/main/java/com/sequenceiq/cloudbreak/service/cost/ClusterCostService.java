package com.sequenceiq.cloudbreak.service.cost;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.cost.co2.CarbonCalculatorService;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.usd.UsdCalculatorService;

@Service
public class ClusterCostService {

    private static final double MAGIC_PROVIDER_COST = 5.3;

    private static final double MAGIC_CLOUDERA_COST = 1.3;

    @Inject
    private CarbonCalculatorService carbonCalculatorService;

    @Inject
    private UsdCalculatorService usdCalculatorService;

    @Inject
    private InstanceTypeCollectorService instanceTypeCollectorService;

    public Map<String, RealTimeCost> getCosts(StackViewV4Responses responses) {
        Map<String, RealTimeCost> realTimeCosts = new HashMap<>();

        //TODO Ricsi / Laci code comes here
        for (StackViewV4Response stack : responses.getResponses()) {
            ClusterCostDto clusterCost = instanceTypeCollectorService.getAllInstanceTypesByCrn(stack.getCrn());
            RealTimeCost realTimeCost = new RealTimeCost();
            realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
            realTimeCost.setType(stack.getType().name());
            realTimeCost.setResourceName(stack.getName());
            realTimeCost.setHourlyProviderUsd(usdCalculatorService.calculateProviderCost(clusterCost));
            realTimeCost.setHourlyClouderaUsd(usdCalculatorService.calculateClouderaCost(clusterCost));
            realTimeCost.setHourlyCO2(carbonCalculatorService.getHourlyCarbonFootPrintByCrn(clusterCost));
            realTimeCosts.put(stack.getCrn(), realTimeCost);
        }
        return realTimeCosts;
    }

}
