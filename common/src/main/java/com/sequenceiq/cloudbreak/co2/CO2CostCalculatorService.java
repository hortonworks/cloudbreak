package com.sequenceiq.cloudbreak.co2;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.co2.model.ClusterCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.DiskCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.InstanceGroupCO2Dto;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Service
public class CO2CostCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CO2CostCalculatorService.class);

    private static final double CPU_UTILIZATION = 0.50;

    private static final double MEMORY_POWER_USAGE_PER_GB = 0.000392;

    private static final double HDD_POWER_USAGE_PER_GB = 0.00065;

    private static final double SSD_POWER_USAGE_PER_GB = 0.00120;

    private static final int ONE_THOUSAND = 1000;

    @Inject
    private Map<CloudPlatform, CO2EmissionFactorService> co2EmissionFactorServiceMap;

    public double calculateCO2InGrams(ClusterCO2Dto clusterCO2Dto) {
        CO2EmissionFactorService co2EmissionFactorService = co2EmissionFactorServiceMap.get(clusterCO2Dto.getCloudPlatform());
        double emissionFactor = co2EmissionFactorService.getEmissionFactorByRegion(clusterCO2Dto.getRegion());
        double co2InGrams = emissionFactor * calculatePowerUsage(clusterCO2Dto) * ONE_THOUSAND;
        LOGGER.info("CO2 equivalent in grams for cluster {} is {}", clusterCO2Dto, co2InGrams);
        return co2InGrams;
    }

    private double calculatePowerUsage(ClusterCO2Dto clusterCO2Dto) {
        double totalWatts = 0.0;
        List<InstanceGroupCO2Dto> instanceGroupCO2DtoList = clusterCO2Dto.getInstanceGroups();
        CO2EmissionFactorService co2EmissionFactorService = co2EmissionFactorServiceMap.get(clusterCO2Dto.getCloudPlatform());
        double averageMinimumWatts = co2EmissionFactorService.getAverageMinimumWatts();
        double averageMaximumWatts = co2EmissionFactorService.getAverageMaximumWatts();
        double powerUsageEffectiveness = co2EmissionFactorService.getPowerUsageEffectiveness();

        for (InstanceGroupCO2Dto instanceGroup : instanceGroupCO2DtoList) {
            if (clusterCO2Dto.isComputeRunning()) {
                totalWatts += calculateComputePowerUsage(instanceGroup, averageMinimumWatts, averageMaximumWatts);
                totalWatts += calculateMemoryPowerUsage(instanceGroup);
            }

            List<DiskCO2Dto> diskCO2DtoList = instanceGroup.getDisksPerInstance();
            for (DiskCO2Dto disk : diskCO2DtoList) {
                totalWatts += instanceGroup.getCount() * calculateStoragePowerUsage(disk);
            }
        }
        double powerUsgae = totalWatts * powerUsageEffectiveness;
        LOGGER.info("Power usage calculated for cluster {} is {}", clusterCO2Dto, powerUsgae);
        return powerUsgae;
    }

    private double calculateComputePowerUsage(InstanceGroupCO2Dto instanceGroupCO2Dto, double averageMinimumWatts, double averageMaximumWatts) {
        double averageWatts = averageMinimumWatts + CPU_UTILIZATION * (averageMaximumWatts - averageMinimumWatts);
        return instanceGroupCO2Dto.getCount() * instanceGroupCO2Dto.getvCPUs() * averageWatts;
    }

    private double calculateMemoryPowerUsage(InstanceGroupCO2Dto instanceGroupCO2Dto) {
        return instanceGroupCO2Dto.getCount() * instanceGroupCO2Dto.getMemory() * MEMORY_POWER_USAGE_PER_GB;
    }

    private double calculateStoragePowerUsage(DiskCO2Dto diskCO2Dto) {
        return diskCO2Dto.getCount() * diskCO2Dto.getSize() * getStoragePowerUsagePerGB(diskCO2Dto.getDiskType());
    }

    private double getStoragePowerUsagePerGB(String diskType) {
        switch (diskType) {
            case "standard":
            case "st1":
            case "sc1":
            case "StandardHDD":
                return HDD_POWER_USAGE_PER_GB;
            default:
                return SSD_POWER_USAGE_PER_GB;
        }
    }
}
