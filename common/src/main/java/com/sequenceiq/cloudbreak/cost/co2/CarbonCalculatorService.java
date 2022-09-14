package com.sequenceiq.cloudbreak.cost.co2;

import java.util.IntSummaryStatistics;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.cost.model.RegionEmissionFactor;
import com.sequenceiq.cloudbreak.common.cost.service.RegionEmissionFactorService;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;

// CHECKSTYLE:OFF
@Service
public class CarbonCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonCalculatorService.class);

    //AWS min/max avg 2.12 Wh/vCPU
    private static final double AWS_AVG_VCPU = 2.12;

    // W/GB
    private static final double AWS_MEMORY_GB = 0.375;

    @Inject
    private RegionEmissionFactorService regionEmissionFactorService;

    public double getHourlyEnergyConsumptionkWhByCrn(ClusterCostDto instanceTypeList) {
        LOGGER.info("Collected instance types: {}", instanceTypeList);
        double summarizedWhConsumption = calculateCpuInWh(instanceTypeList) + calculateMemoryInWh(instanceTypeList) + calculateDiskInWh();
        return summarizedWhConsumption / 1000.0;
    }
    public double getHourlyCarbonFootPrintByCrn(ClusterCostDto instanceTypeList) {
        RegionEmissionFactor emissionFactor = getCo2RateForRegion(instanceTypeList);
        LOGGER.info("RegionEmissionFactor for region {} is: {}", instanceTypeList.getRegion(), emissionFactor);
        return getHourlyEnergyConsumptionkWhByCrn(instanceTypeList) * emissionFactor.getCo2e() * 1000000.0;
    }

    private double calculateCpuInWh(ClusterCostDto dto) {
        IntSummaryStatistics vCpuCount = dto.getInstanceGroups().stream().collect(Collectors.summarizingInt(InstanceGroupCostDto::getTotalvCpuCores));
        LOGGER.info("Cluster vCpuCount is: {}", vCpuCount);
        return vCpuCount.getSum() * AWS_AVG_VCPU;
    }

    private double calculateDiskInWh() {
        LOGGER.info("Skipping Disk calculation and counting with 0");
        return 0;
    }

    private double calculateMemoryInWh(ClusterCostDto dto) {
        IntSummaryStatistics totalMemoryCountInGb = dto.getInstanceGroups().stream().collect(Collectors.summarizingInt(InstanceGroupCostDto::getTotalMemoryInGb));
        LOGGER.info("Cluster memorySize in GB: {}", totalMemoryCountInGb);
        return totalMemoryCountInGb.getSum() * AWS_MEMORY_GB;
    }

    private RegionEmissionFactor getCo2RateForRegion(ClusterCostDto dto) {
        return regionEmissionFactorService.get(dto.getRegion());
    }
}
