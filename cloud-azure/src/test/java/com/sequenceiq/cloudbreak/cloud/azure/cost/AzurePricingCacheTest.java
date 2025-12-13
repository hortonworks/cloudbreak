package com.sequenceiq.cloudbreak.cloud.azure.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
class AzurePricingCacheTest {

    private static final String REGION = "region";

    private static final String INSTANCE_TYPE = "instanceType";

    private static final double AZURE_DEFAULT_STORAGE_PRICE = 0.008180555556;

    @InjectMocks
    private AzurePricingCache underTest;

    @Mock
    private CloudParameterService cloudParameterService;

    @Test
    void getUsdPrice() {
        AzurePricingCache spiedUnderTest = spy(underTest);
        doReturn(getPriceResponse()).when(spiedUnderTest).retryableGetPriceResponse(any());

        Optional<Double> price = spiedUnderTest.getPriceForInstanceType(REGION, INSTANCE_TYPE, null);

        assertTrue(price.isPresent());
        assertEquals(0.69, price.get());
    }

    @Test
    void getCpuCount() {
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(getCloudVmTypes());

        Optional<Integer> cpu = underTest.getCpuCountForInstanceType(REGION, INSTANCE_TYPE, null);

        assertTrue(cpu.isPresent());
        assertEquals(69, cpu.get());
    }

    @Test
    void getMemory() {
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(getCloudVmTypes());

        Optional<Integer> memory = underTest.getMemoryForInstanceType(REGION, INSTANCE_TYPE, null);

        assertTrue(memory.isPresent());
        assertEquals(420, memory.get());
    }

    @Test
    void getStoragePricePerGBHour() {
        Optional<Double> premiumssd = underTest.getStoragePricePerGBHour("westus2", "PremiumSSD_LRS", 500);
        Optional<Double> standardssd = underTest.getStoragePricePerGBHour("westus2", "StandardSSD_LRS", 500);
        Optional<Double> standardhdd = underTest.getStoragePricePerGBHour("westus2", "StandardHDD", 500);

        assertTrue(premiumssd.isPresent());
        assertTrue(standardssd.isPresent());
        assertTrue(standardhdd.isPresent());
        assertEquals(0.00018489, premiumssd.get(), 0.00001);
        assertEquals(0.00010666, standardssd.get(), 0.00001);
        assertEquals(0.00006044, standardhdd.get(), 0.00001);
    }

    @Test
    void getUsdPriceAlreadyInCache() {
        AzurePricingCache spiedUnderTest = spy(underTest);
        doReturn(getPriceResponse()).when(spiedUnderTest).retryableGetPriceResponse(any());

        Optional<Double> price1 = spiedUnderTest.getPriceForInstanceType(REGION, INSTANCE_TYPE, null);
        Optional<Double> price2 = spiedUnderTest.getPriceForInstanceType(REGION, INSTANCE_TYPE, null);

        assertTrue(price1.isPresent());
        assertTrue(price2.isPresent());
        assertEquals(0.69, price1.get());
        assertEquals(0.69, price2.get());
    }

    @Test
    void getCpuCountAlreadyInCache() {
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(getCloudVmTypes());

        Optional<Integer> cpu1 = underTest.getCpuCountForInstanceType(REGION, INSTANCE_TYPE, null);
        Optional<Integer> cpu2 = underTest.getCpuCountForInstanceType(REGION, INSTANCE_TYPE, null);

        assertTrue(cpu1.isPresent());
        assertTrue(cpu2.isPresent());
        assertEquals(69, cpu1.get());
        assertEquals(69, cpu2.get());
    }

    @Test
    void getStoragePriceWithZeroVolumeSize() {
        Optional<Double> price = underTest.getStoragePricePerGBHour("eastus", "StandardSSD_LRS", 0);

        assertTrue(price.isEmpty());
    }

    @Test
    void getStoragePriceWithNullStorageType() {
        Optional<Double> price = underTest.getStoragePricePerGBHour("eastus", null, 100);

        assertTrue(price.isPresent());
        assertEquals(AZURE_DEFAULT_STORAGE_PRICE / 100, price.get(), 0.00001);
    }

    @Test
    void getStoragePriceWithUnknownStorageType() {
        Optional<Double> price = underTest.getStoragePricePerGBHour("eastus", "unknown", 100);

        assertTrue(price.isPresent());
        assertEquals(AZURE_DEFAULT_STORAGE_PRICE / 100, price.get(), 0.00001);
    }

    @Test
    void getStoragePrice() {
        Optional<Double> price = underTest.getStoragePricePerGBHour("eastus", "StandardSSD_LRS", 1000);

        assertTrue(price.isPresent());
        assertNotEquals(0.0, price.get());
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
