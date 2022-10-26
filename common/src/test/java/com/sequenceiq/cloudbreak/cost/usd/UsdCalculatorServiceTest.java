package com.sequenceiq.cloudbreak.cost.usd;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.DiskCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;

@ExtendWith(MockitoExtension.class)
public class UsdCalculatorServiceTest {

    @InjectMocks
    private UsdCalculatorService underTest;

    @Test
    public void calculateProviderCost() {
        double providerCost = underTest.calculateProviderCost(getClusterCostDto());

        Assertions.assertEquals(5.00, providerCost);
    }

    @Test
    public void calculateClouderaCost() {
        double clouderaCost = underTest.calculateClouderaCost(getClusterCostDto(), "WORKLOAD");

        Assertions.assertEquals(6.00, clouderaCost);
    }

    private ClusterCostDto getClusterCostDto() {
        // provider cost per disk: 2 * 100 * 0,01 = 2.00
        DiskCostDto diskCostDto = new DiskCostDto();
        diskCostDto.setCount(2);
        diskCostDto.setSize(100);
        diskCostDto.setPricePerDiskGB(0.01);

        // provider cost per instance group: (6 * 0.50) + 2.00 = 5.00
        InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
        instanceGroupCostDto.setCount(6);
        instanceGroupCostDto.setPricePerInstance(0.50);
        instanceGroupCostDto.setDisksPerInstance(List.of(diskCostDto));

        // cloudera cost per instance group: 6 * 1.00 = 6.00
        instanceGroupCostDto.setClouderaPricePerInstance(1.00);

        ClusterCostDto clusterCostDto = new ClusterCostDto();
        clusterCostDto.setStatus("AVAILABLE");
        clusterCostDto.setInstanceGroups(List.of(instanceGroupCostDto));
        return clusterCostDto;
    }
}
