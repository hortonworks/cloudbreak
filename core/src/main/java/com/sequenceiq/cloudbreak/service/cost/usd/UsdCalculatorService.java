package com.sequenceiq.cloudbreak.service.cost.usd;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.service.cost.model.InstanceGroupCostDto;

@Service
public class UsdCalculatorService {

    public double calculateProviderCost(ClusterCostDto clusterCostDto) {
        double price = 0.0;
        for (InstanceGroupCostDto instanceGroup : clusterCostDto.getInstanceGroups()) {
            price += instanceGroup.getCount() * instanceGroup.getPricePerInstance();
        }
        return price;
    }

    public double calculateClouderaCost(ClusterCostDto clusterCostDto) {
        double price = 0.0;
        for (InstanceGroupCostDto instanceGroup : clusterCostDto.getInstanceGroups()) {
            price += instanceGroup.getCount() * instanceGroup.getClouderaPricePerInstance();
        }
        return price;
    }
}
