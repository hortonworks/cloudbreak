package com.sequenceiq.cloudbreak.cost.usd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.DiskCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;

@Service
public class UsdCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsdCalculatorService.class);

    private static final String WORKLOAD = "WORKLOAD";

    public double calculateProviderCost(ClusterCostDto clusterCostDto) {
        double price = 0.0;
        for (InstanceGroupCostDto instanceGroup : clusterCostDto.getInstanceGroups()) {
            if (clusterCostDto.isComputeRunning()) {
                price += instanceGroup.getTotalProviderPrice();
            }
            for (DiskCostDto diskCostDto : instanceGroup.getDisksPerInstance()) {
                price += instanceGroup.getCount() * diskCostDto.getTotalDiskPrice();
            }
        }
        LOGGER.info("Provider cost calculated for cluster {} is {}", clusterCostDto, price);
        return price;
    }

    public double calculateClouderaCost(ClusterCostDto clusterCostDto, String type) {
        double price = 0.0;
        if (WORKLOAD.equals(type) && clusterCostDto.isComputeRunning()) {
            for (InstanceGroupCostDto instanceGroup : clusterCostDto.getInstanceGroups()) {
                price += instanceGroup.getTotalClouderaPrice();
            }
        }
        LOGGER.info("Cloudera cost calculated for cluster {} is {}", clusterCostDto, price);
        return price;
    }
}
