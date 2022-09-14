package com.sequenceiq.cloudbreak.service.cost.co2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

// CHECKSTYLE:OFF
@Service
public class CarbonCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonCalculatorService.class);
    //AWS min/max avg 2.12 Wh/vCPU
    private static final double AWS_AVG_VCPU = 2.12;
    //g/kWh
    private static final double CO2_RATE_CALIFORNIA = 347;
    @Inject
    private StackRepository stackRepository;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    public double getHourlyCarbonFootPrintByCrn(String crn) {
        LOGGER.info("Calculating CO2FP for resource: {}", crn);
        //filter nodes that are not in available status
        Map<String, Long> instanceTypeList = getAllInstanceTypesByCrn(crn);
        LOGGER.info("Collected instnace types: {}", instanceTypeList);
        double summarizedWhConsumption = calculateCpuInWh() + calculateDiskInWh() + calculateMemoryInWh();
        // get cluster proper region for CO2 rate
        return summarizedWhConsumption * getCo2RateForRegion() / 1000;
    }

    private Map<String, Long> getAllInstanceTypesByCrn(String crn) {
        Stack stack = stackRepository.findByResourceCrn(crn).get();
        //get list of all instancetype
        List<InstanceGroupView> instanceGroupList = instanceGroupService.getInstanceGroupViewByStackId(stack.getId());
        Map<String, Long> countByInstanceType = instanceGroupList.stream()
                .collect(Collectors.groupingBy(ig -> ig.getTemplate().getInstanceType(), Collectors.counting()));
        return countByInstanceType;
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
