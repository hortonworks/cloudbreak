package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.models.VirtualMachineSize;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VmType;

@ExtendWith(MockitoExtension.class)
class AzurePlatformResourcesTest {

    @Mock
    private AzureClient azureClient;

    @Mock
    private ExtendedCloudCredential cloudCredential;

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
        when(azureClient.getAvailabilityZones(region.value())).thenReturn(Map.of());

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
        virtualMachineSizes.forEach(virtualMachineSize -> {
            List<String> availabilityZones = actual.getCloudVmResponses()
                    .get(region.value())
                    .stream()
                    .filter(vmType -> virtualMachineSize.name().equals(vmType.value()))
                    .findFirst()
                    .map(vmType -> vmType.getMetaData().getAvailabilityZones())
                    .orElse(null);
            assertEquals(availabilityZones, Collections.emptyList());

        });
    }

    @Test
    void testVirtualMachinesWhenInstanceTypeHasZeroResourceDiskMustBePresented() {
        Region region = Region.region("westeruope");
        Set<VirtualMachineSize> virtualMachineSizes = new HashSet<>();
        VirtualMachineSize d2sTypeWithoutResourceDisk = createVirtualMachineSize("Standard_D2s_v4", 0);
        VirtualMachineSize e64sVmTypeWithoutResourceDisk = createVirtualMachineSize("Standard_E64s_v4", 0);
        virtualMachineSizes.add(createVirtualMachineSize("Standard_DS2_v2", 20000));
        virtualMachineSizes.add(d2sTypeWithoutResourceDisk);
        virtualMachineSizes.add(createVirtualMachineSize("Standard_E64ds_v4", 1400000));
        virtualMachineSizes.add(e64sVmTypeWithoutResourceDisk);
        when(azureClient.getVmTypes(region.value())).thenReturn(virtualMachineSizes);
        Map<String, List<String>> zoneInfo = new HashMap<>();
        zoneInfo.put("Standard_D2s_v4", List.of("1", "2"));
        zoneInfo.put("Standard_E64s_v4", List.of("2", "3"));
        zoneInfo.put("Standard_DS2_v2", List.of("1"));
        zoneInfo.put("Standard_E64ds_v4", List.of("1", "2", "3"));
        when(azureClient.getAvailabilityZones(region.value())).thenReturn(zoneInfo);

        CloudVmTypes actual = underTest.virtualMachines(cloudCredential, region, Map.of());

        assertNotNull(actual);
        assertNotNull(actual.getCloudVmResponses());
        assertFalse(actual.getCloudVmResponses().isEmpty());
        assertNotNull(actual.getCloudVmResponses().get(region.value()));
        assertEquals(virtualMachineSizes.size(), actual.getCloudVmResponses().get(region.value()).size());
        virtualMachineSizes.forEach(virtualMachineSize -> {
            VmType matchingVm = actual.getCloudVmResponses()
                    .get(region.value())
                    .stream()
                    .filter(vmType -> virtualMachineSize.name().equals(vmType.value()))
                    .findFirst()
                    .orElse(null);
            if (matchingVm == null) {
                fail("VM from input is not present in response");
            }
            assertEquals(zoneInfo.get(matchingVm.value()), matchingVm.getMetaData().getAvailabilityZones());
        });
    }

    @Test
    void collectInstanceStorageCountTest() {
        Region region = Region.region("us-west-1");
        CloudContext cloudContext = new CloudContext.Builder()
                .withLocation(Location.location(region, availabilityZone("us-west-1")))
                .build();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        Set<VirtualMachineSize> virtualMachineSizes = new HashSet<>();
        virtualMachineSizes.add(createVirtualMachineSize("Standard_D8_v3", 20000));
        when(azureClient.getVmTypes(region.value())).thenReturn(virtualMachineSizes);

        InstanceStoreMetadata instanceStoreMetadata = underTest.collectInstanceStorageCount(ac, Collections.singletonList("Standard_D8_v3"));

        assertEquals(1, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("Standard_D8_v3"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("unsupported"));
    }

    @Test
    void collectInstanceStorageCountWhenInstanceTypeIsNotFoundTest() {
        Region region = Region.region("us-west-1");
        CloudContext cloudContext = new CloudContext.Builder()
                .withLocation(Location.location(region, availabilityZone("us-west-1")))
                .build();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        Set<VirtualMachineSize> virtualMachineSizes = new HashSet<>();
        when(azureClient.getVmTypes(region.value())).thenReturn(virtualMachineSizes);

        InstanceStoreMetadata instanceStoreMetadata = underTest.collectInstanceStorageCount(ac, Collections.singletonList("unsupported"));

        assertNull(instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("unsupported"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("unsupported"));
        assertNull(instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("Standard_D8_v3"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("Standard_D8_v3"));

        when(azureClient.getVmTypes(region.value())).thenReturn(virtualMachineSizes);

        instanceStoreMetadata = underTest.collectInstanceStorageCount(ac, new ArrayList<>());

        assertNull(instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("Standard_D8_v3"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("Standard_D8_v3"));
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
            public VirtualMachineSizeTypes virtualMachineSizeType() {
                return VirtualMachineSizeTypes.BASIC_A1;
            }

            @Override
            public String name() {
                return name;
            }
        };
    }
}