package com.sequenceiq.cloudbreak.cloud.azure.cost;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.cost.model.PriceDetails;
import com.sequenceiq.cloudbreak.cloud.azure.cost.model.PriceResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;

@ExtendWith(MockitoExtension.class)
public class AzurePricingCacheTest {

    private static final String REGION = "region";

    private static final String INSTANCE_TYPE = "instanceType";

    @InjectMocks
    private AzurePricingCache underTest;

    @Mock
    private CloudParameterService cloudParameterService;

    @Test
    void getUsdPrice() {
        AzurePricingCache spiedUnderTest = spy(underTest);
        doReturn(getPriceResponse()).when(spiedUnderTest).retryableGetPriceResponse(any());

        double price = spiedUnderTest.getPriceForInstanceType(REGION, INSTANCE_TYPE);

        Assertions.assertEquals(0.69, price);
    }

    @Test
    void getCpuCount() {
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(getCloudVmTypes());

        int cpu = underTest.getCpuCountForInstanceType(REGION, INSTANCE_TYPE, null);

        Assertions.assertEquals(69, cpu);
    }

    @Test
    void getMemory() {
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(getCloudVmTypes());

        int memory = underTest.getMemoryForInstanceType(REGION, INSTANCE_TYPE, null);

        Assertions.assertEquals(420, memory);
    }

    @Test
    void getUsdPriceAlreadyInCache() {
        AzurePricingCache spiedUnderTest = spy(underTest);
        doReturn(getPriceResponse()).when(spiedUnderTest).retryableGetPriceResponse(any());

        double price1 = spiedUnderTest.getPriceForInstanceType(REGION, INSTANCE_TYPE);
        double price2 = spiedUnderTest.getPriceForInstanceType(REGION, INSTANCE_TYPE);

        Assertions.assertEquals(0.69, price1);
        Assertions.assertEquals(0.69, price2);
    }

    @Test
    void getCpuCountAlreadyInCache() {
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(getCloudVmTypes());

        int cpu1 = underTest.getCpuCountForInstanceType(REGION, INSTANCE_TYPE, null);
        int cpu2 = underTest.getCpuCountForInstanceType(REGION, INSTANCE_TYPE, null);

        Assertions.assertEquals(69, cpu1);
        Assertions.assertEquals(69, cpu2);
    }

    private PriceResponse getPriceResponse() {
        PriceDetails priceDetails = new PriceDetails();
        priceDetails.setRetailPrice(0.69);
        return new PriceResponse(null, null, null, List.of(priceDetails), null, 1);
    }

    private CloudVmTypes getCloudVmTypes() {
        VmTypeMeta vmTypeMeta = new VmTypeMeta();
        vmTypeMeta.setProperties(Map.of("Cpu", 69, "Memory", 420));
        VmType vmType = VmType.vmTypeWithMeta(INSTANCE_TYPE, vmTypeMeta, false);
        return new CloudVmTypes(Map.of(REGION, Set.of(vmType)), null);
    }
}
