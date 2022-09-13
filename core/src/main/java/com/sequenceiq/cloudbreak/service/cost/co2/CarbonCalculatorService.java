package com.sequenceiq.cloudbreak.service.cost.co2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CarbonCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonCalculatorService.class);

    //AWS min/max avg 2.12 Wh/vCPU
    private static final double AWS_AVG_VCPU = 2.12;

    //g/kWh
    private static final double CO2_RATE_CALIFORNIA = 347;

    public double getHourlyCarbonFootPrintByCrn(String crn) {
        LOGGER.info("Calculating CO2FP for resource: {}", crn);
        //get list of all instancetype
        //filter nodes that are not in available status
        double summarizedWhConsumption = calculateCpuInWh() + calculateDiskInWh() + calculateMemoryInWh();
        // get cluster proper region for CO2 rate
        return summarizedWhConsumption * getCo2RateForRegion() / 1000;
    }

    private double calculateCpuInWh() {
        return 20 * AWS_AVG_VCPU;
    }

    private double calculateDiskInWh() {
        return 5.0;
    }

    private double calculateMemoryInWh() {
        return 3.0;
    }

    private double getCo2RateForRegion() {
        return CO2_RATE_CALIFORNIA;
    }


    private double getVcpuCountForCluster() {
        return 300;
    }

}
