package com.sequenceiq.cloudbreak.service.cost.co2;
// CHECKSTYLE:OFF

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.cost.model.RegionEmissionFactor;
import com.sequenceiq.cloudbreak.common.cost.service.RegionEmissionFactorService;
import com.sequenceiq.cloudbreak.cost.co2.CarbonCalculatorService;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;

@ExtendWith(MockitoExtension.class)
class CarbonCalculatorServiceTest {

    @Mock
    private RegionEmissionFactorService regionEmissionFactorService;

    @InjectMocks
    private CarbonCalculatorService underTest;

    @Test
    void getHourlyCarbonFootPrintByCrn() {
        //TODO: add some usecases
        ClusterCostDto testDto = new ClusterCostDto();
        RegionEmissionFactor emissionFactor = new RegionEmissionFactor();
        emissionFactor.setCo2e(0.0001234);
        when(regionEmissionFactorService.get(any())).thenReturn(emissionFactor);
        assertEquals(6.972716999999999, underTest.getHourlyCarbonFootPrintByCrn(createClusterCostDto(3, 5)));
        assertEquals(8.360966999999999, underTest.getHourlyCarbonFootPrintByCrn(createClusterCostDto(3, 15)));
    }

    private ClusterCostDto createClusterCostDto(int instanceCount, int memorySize) {
        ClusterCostDto testDto = new ClusterCostDto();
        testDto.setRegion("us-west-1");
        List<InstanceGroupCostDto> instanceGroupCostDtos = new ArrayList<>();
        InstanceGroupCostDto singleDto = new InstanceGroupCostDto();
        singleDto.setCount(instanceCount);
        singleDto.setCoresPerInstance(8);
        singleDto.setType("m5.xlarge");
        singleDto.setMemoryPerInstance(memorySize);
        instanceGroupCostDtos.add(singleDto);
        testDto.setInstanceGroups(instanceGroupCostDtos);
        return testDto;
    }
}