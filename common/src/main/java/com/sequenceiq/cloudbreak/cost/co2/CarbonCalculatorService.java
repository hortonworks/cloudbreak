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

    @Inject
    private RegionEmissionFactorService regionEmissionFactorService;

    public double getHourlyCarbonFootPrintByCrn(ClusterCostDto instanceTypeList) {
        //filter nodes that are not in available status
        LOGGER.info("Collected instance types: {}", instanceTypeList);
        double summarizedWhConsumption = calculateCpuInWh(instanceTypeList) + calculateDiskInWh() + calculateMemoryInWh();
        // get cluster proper region for CO2 rate
        RegionEmissionFactor emissionFactor = getCo2RateForRegion(instanceTypeList);
        LOGGER.info("RegionEmissionFactor for region {} is: {}", instanceTypeList.getRegion(), emissionFactor);
        return summarizedWhConsumption / 1000.0 * emissionFactor.getCo2e() * 1000000.0;
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

    private double calculateMemoryInWh() {
        LOGGER.info("Skipping Memory calculation and counting with 0");
        return 0;
    }

    private RegionEmissionFactor getCo2RateForRegion(ClusterCostDto dto) {
        return regionEmissionFactorService.get(dto.getRegion());
    }
}
