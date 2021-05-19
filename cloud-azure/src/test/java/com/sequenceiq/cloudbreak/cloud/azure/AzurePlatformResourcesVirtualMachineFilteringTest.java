package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
class AzurePlatformResourcesVirtualMachineFilteringTest {

    @Mock
    private AzureClient azureClient;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private AzureClientService azureClientService;

    @InjectMocks
    private AzurePlatformResources underTest;

    @BeforeEach
    void setUp() {
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
    }

    @Test
    void testVirtualMachinesWhenNoInstanceTypeShouldBeFilteredOut() {
        Region region = Region.region("westeruope");
        Set<VirtualMachineSize> virtualMachineSizes = new HashSet<>();
        virtualMachineSizes.add(createVirtualMachineSize("Standard_DS2_v2", 20000));
        virtualMachineSizes.add(createVirtualMachineSize("Standard_E64ds_v4", 1400000));
        when(azureClient.getVmTypes(region.value())).thenReturn(virtualMachineSizes);

        CloudVmTypes actual = underTest.virtualMachines(cloudCredential, region, Map.of());

        assertNotNull(actual);
        assertNotNull(actual.getCloudVmResponses());
        assertFalse(actual.getCloudVmResponses().isEmpty());
        assertNotNull(actual.getCloudVmResponses().get(region.value()));
        assertEquals(virtualMachineSizes.size(), actual.getCloudVmResponses().get(region.value()).size());
        virtualMachineSizes
                .forEach(virtualMachineSize -> {
                    boolean virtualMachineSizeCouldBeFoundInResult = actual.getCloudVmResponses()
                            .get(region.value())
                            .stream()
                            .anyMatch(cloudVMResponse -> virtualMachineSize.name().equals(cloudVMResponse.value()));
                    assertTrue(virtualMachineSizeCouldBeFoundInResult);
                });
    }

    @Test
    void testVirtualMachinesWhenInstanceTypeShouldBeFilteredOut() {
        Region region = Region.region("westeruope");
        Set<VirtualMachineSize> virtualMachineSizes = new HashSet<>();
        VirtualMachineSize d2sTypeWithoutResourceDisk = createVirtualMachineSize("Standard_D2s_v4", 0);
        VirtualMachineSize e64sVmTypeWithoutResourceDisk = createVirtualMachineSize("Standard_E64s_v4", 0);
        virtualMachineSizes.add(createVirtualMachineSize("Standard_DS2_v2", 20000));
        virtualMachineSizes.add(d2sTypeWithoutResourceDisk);
        virtualMachineSizes.add(createVirtualMachineSize("Standard_E64ds_v4", 1400000));
        virtualMachineSizes.add(e64sVmTypeWithoutResourceDisk);
        when(azureClient.getVmTypes(region.value())).thenReturn(virtualMachineSizes);

        CloudVmTypes actual = underTest.virtualMachines(cloudCredential, region, Map.of());

        Set<VirtualMachineSize> vmTypeWithoutResourceDisk = Set.of(d2sTypeWithoutResourceDisk, e64sVmTypeWithoutResourceDisk);
        assertNotNull(actual);
        assertNotNull(actual.getCloudVmResponses());
        assertFalse(actual.getCloudVmResponses().isEmpty());
        assertNotNull(actual.getCloudVmResponses().get(region.value()));
        assertEquals(virtualMachineSizes.size() - vmTypeWithoutResourceDisk.size(), actual.getCloudVmResponses().get(region.value()).size());
        boolean resultContainsVmTypesWithoutResourceDisk = actual.getCloudVmResponses()
                .get(region.value())
                .stream()
                .anyMatch(cloudVMResponse -> vmTypeWithoutResourceDisk.stream().anyMatch(vmSize -> vmSize.name().equals(cloudVMResponse.value())));
        assertFalse(resultContainsVmTypesWithoutResourceDisk);
    }

    private VirtualMachineSize createVirtualMachineSize(String name, int resourceDiskSizeInMB) {
        return new VirtualMachineSize() {
            @Override
            public int numberOfCores() {
                return 0;
            }

            @Override
            public int osDiskSizeInMB() {
                return 0;
            }

            @Override
            public int resourceDiskSizeInMB() {
                return resourceDiskSizeInMB;
            }

            @Override
            public int memoryInMB() {
                return 0;
            }

            @Override
            public int maxDataDiskCount() {
                return 0;
            }

            @Override
            public String name() {
                return name;
            }
        };
    }
}