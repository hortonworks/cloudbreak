package com.sequenceiq.cloudbreak.service.cost;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;

@Service
public class ClusterCostService {

    private static final double MAGIC_PROVIDER_COST = 5.3;

    private static final double MAGIC_CLOUDERA_COST = 1.3;

    private static final double MAGIC_CO2 = 122.1;

    public List<RealTimeCost> getCosts(StackViewV4Responses responses) {
        List<RealTimeCost> realTimeCosts = new ArrayList<>();

        //TODO Ricsi / Laci code comes here
        for (StackViewV4Response stack : responses.getResponses()) {
            RealTimeCost realTimeCost = new RealTimeCost();
            realTimeCost.setEnvCrn(stack.getEnvironmentCrn());
            realTimeCost.setResourceCrn(stack.getCrn());
            realTimeCost.setHourlyProviderUsd(MAGIC_PROVIDER_COST);
            realTimeCost.setHourlyClouderaUsd(MAGIC_CLOUDERA_COST);
            realTimeCost.setHourlyCO2(MAGIC_CO2);
            realTimeCosts.add(realTimeCost);
        }
        return realTimeCosts;
    }
}
