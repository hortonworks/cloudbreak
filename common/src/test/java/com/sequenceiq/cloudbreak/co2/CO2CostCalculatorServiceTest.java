package com.sequenceiq.cloudbreak.co2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.co2.model.ClusterCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.DiskCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.InstanceGroupCO2Dto;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@ExtendWith(MockitoExtension.class)
public class CO2CostCalculatorServiceTest {

    @Mock
    private Map<CloudPlatform, CO2EmissionFactorService> co2EmissionFactorServiceMap;

    @Mock
    private CO2EmissionFactorService co2EmissionFactorService;

    @InjectMocks
    private CO2CostCalculatorService underTest;

    @Test
    void calculateCO2InGrams() {
        when(co2EmissionFactorServiceMap.get(CloudPlatform.AWS)).thenReturn(co2EmissionFactorService);
        when(co2EmissionFactorService.getEmissionFactorByRegion(any())).thenReturn(0.000322167);
        when(co2EmissionFactorService.getAverageMinimumWatts()).thenReturn(0.74);
        when(co2EmissionFactorService.getAverageMaximumWatts()).thenReturn(3.5);
        when(co2EmissionFactorService.getPowerUsageEffectiveness()).thenReturn(1.135);

        double result = underTest.calculateCO2InGrams(getClusterCO2Dto());

        assertEquals(12.5311847852, result, 0.00001);
    }

    private ClusterCO2Dto getClusterCO2Dto() {
        // 2 instance, 1 disk per instance -> 2 disks total
        // 2 * 1 * 250 * 0.00065 = 0.325 Wh
        DiskCO2Dto diskCO2Dto = new DiskCO2Dto();
        diskCO2Dto.setDiskType("standard");
        diskCO2Dto.setSize(250);
        diskCO2Dto.setCount(1);

        // Compute: 2 * 8 * (0.74 + 0.50 * (3.5 - 0.74)) = 33.92 Wh
        // Memory: 2 * 32 * 0.000392 = 0.025088 Wh
        InstanceGroupCO2Dto instanceGroupCO2Dto = new InstanceGroupCO2Dto();
        instanceGroupCO2Dto.setCount(2);
        instanceGroupCO2Dto.setvCPUs(8);
        instanceGroupCO2Dto.setMemory(32);
        instanceGroupCO2Dto.setDisksPerInstance(List.of(diskCO2Dto));

        // Total adjusted by PUE: 1.135 * (0.325 + 33.92 + 0.025088) = 38.89654988 Wh
        ClusterCO2Dto clusterCO2Dto = new ClusterCO2Dto();
        clusterCO2Dto.setStatus("AVAILABLE");
        clusterCO2Dto.setRegion("us-west-2");
        clusterCO2Dto.setCloudPlatform(CloudPlatform.AWS);
        clusterCO2Dto.setInstanceGroups(List.of(instanceGroupCO2Dto));

        // CO2 equivalent in grams: 38.89654988 * 0.000322167 * 1000 = 12.5311847852 g
        return clusterCO2Dto;
    }
}
