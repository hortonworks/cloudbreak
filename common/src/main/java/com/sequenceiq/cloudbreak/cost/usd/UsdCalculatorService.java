package com.sequenceiq.cloudbreak.cost.usd;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;

@Service
public class UsdCalculatorService {

    public double calculateProviderCost(ClusterCostDto clusterCostDto) {
        double price = 0.0;
        for (InstanceGroupCostDto instanceGroup : clusterCostDto.getInstanceGroups()) {
            price += instanceGroup.getTotalProviderPrice();
        }
        return price;
    }

    public double calculateClouderaCost(ClusterCostDto clusterCostDto) {
        double price = 0.0;
        for (InstanceGroupCostDto instanceGroup : clusterCostDto.getInstanceGroups()) {
            price += instanceGroup.getClouderaPricePerInstance();
        }
        return price;
    }
}
