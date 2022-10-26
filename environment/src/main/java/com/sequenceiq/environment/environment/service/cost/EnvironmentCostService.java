package com.sequenceiq.environment.environment.service.cost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.ClusterCostV4Endpoint;
import com.sequenceiq.cloudbreak.common.cost.EnvironmentRealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.freeipa.api.v1.cost.FreeIpaCostV1Endpoint;

@Service
public class EnvironmentCostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCostService.class);

    @Inject
    private ClusterCostV4Endpoint clusterCostV4Endpoint;

    @Inject
    private FreeIpaCostV1Endpoint freeIpaCostV1Endpoint;

    public Map<String, EnvironmentRealTimeCost> getCosts(List<EnvironmentDto> environmentDtos) {
        Map<String, EnvironmentRealTimeCost> environmentCost = new HashMap<>();
        Map<String, RealTimeCost> totalCosts = new HashMap<>();
        totalCosts.putAll(clusterCostV4Endpoint.list().getCost());
        totalCosts.putAll(freeIpaCostV1Endpoint.list().getCost());
        LOGGER.debug("Total Costs: {}", totalCosts);
        for (Map.Entry<String, RealTimeCost> costEntry : totalCosts.entrySet()) {
            String key = costEntry.getKey();
            RealTimeCost dhCost = costEntry.getValue();
            String envCrn = costEntry.getValue().getEnvCrn();

            EnvironmentRealTimeCost cost = environmentCost.getOrDefault(envCrn, new EnvironmentRealTimeCost());
            cost.add(key, dhCost);
            environmentCost.put(envCrn, cost);
        }
        LOGGER.debug("Cost summed for environments: {}", environmentCost);
        return environmentCost;
    }
}
