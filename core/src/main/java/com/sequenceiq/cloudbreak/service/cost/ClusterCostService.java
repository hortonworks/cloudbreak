package com.sequenceiq.cloudbreak.service.cost;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.banzai.BanzaiCache;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.service.cost.co2.CarbonCalculatorService;

@Service
public class ClusterCostService {

    private static final double MAGIC_PROVIDER_COST = 5.3;

    private static final double MAGIC_CLOUDERA_COST = 1.3;

    @Inject
    private CarbonCalculatorService carbonCalculatorService;

    @Inject
    private BanzaiCache banzaiCache;

    public Map<String, RealTimeCost> getCosts(StackViewV4Responses responses) {
        Map<String, RealTimeCost> realTimeCosts = new HashMap<>();

        //TODO Ricsi / Laci code comes here
        for (StackViewV4Response stack : responses.getResponses()) {
            RealTimeCost realTimeCost = new RealTimeCost();
            realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
            realTimeCost.setHourlyProviderUsd(MAGIC_PROVIDER_COST);
            realTimeCost.setHourlyClouderaUsd(MAGIC_CLOUDERA_COST);
            realTimeCost.setHourlyCO2(carbonCalculatorService.getHourlyCarbonFootPrintByCrn(stack.getCrn()));
            realTimeCosts.put(stack.getCrn(), realTimeCost);
        }
        return realTimeCosts;
    }

}
