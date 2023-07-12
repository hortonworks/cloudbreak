package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;

@ExtendWith(MockitoExtension.class)
public class AzureAvailabilityZoneConnectorTest {
    @Mock
    private AzurePlatformResources azurePlatformResources;

    @InjectMocks
    private AzureAvailabilityZoneConnector azureAvailabilityZoneConnector;

    @Test
    void testGetAvailabilityZonesWithNoResponseForGivenRegion() {
        when(azurePlatformResources.virtualMachines(any(), any(), any())).thenReturn(getCloudVmTypes(Collections.emptyMap()));
        Set<String> availabilityZones = azureAvailabilityZoneConnector.getAvailabilityZones(null, Set.of("1", "2", "3"),
                "Standard_DS2_v2", Region.region("westus1"));
        assertEquals(Collections.emptySet(), availabilityZones);
    }

    static Object [] [] dataForTestGetAvailabilityZones() {
        return new Object[] [] {
                {Set.of("1", "2", "3"), Collections.emptySet(), Collections.emptySet()},
                {Collections.emptySet(), Set.of("1", "2", "3"), Collections.emptySet()},
                {null, Set.of("1", "2", "3"), Collections.emptySet()},
                {Set.of("1", "2", "3"), Set.of("1"), Set.of("1")},
                {Set.of("1", "2"), Set.of("2", "3"), Set.of("2")}
        };
    }

    @ParameterizedTest(name = "testGetAvailabilityZones")
    @MethodSource("dataForTestGetAvailabilityZones")
    void testGetAvailabilityZones(Set<String> environmentZones, Set<String> zonesSupportedForInstanceType, Set<String> expectedZones) {
        when(azurePlatformResources.virtualMachines(any(), any(), any())).thenReturn(getCloudVmTypes(Map.of("Standard_DS2_v2",
                zonesSupportedForInstanceType)));
        Set<String> availabilityZones = azureAvailabilityZoneConnector.getAvailabilityZones(null, environmentZones, "Standard_DS2_v2",
                Region.region("westus2"));
        assertEquals(expectedZones, availabilityZones);
    }

    @Test
    void testGetAvailabilityZonesWithNoResponseForVirtualMachine() {
        when(azurePlatformResources.virtualMachines(any(), any(), any())).thenReturn(getCloudVmTypes(Map.of("Standard_DS2_v1", Set.of("1", "2", "3"))));
        Set<String> availabilityZones = azureAvailabilityZoneConnector.getAvailabilityZones(null, Set.of("1", "2", "3"),
                "Standard_DS2_v2", Region.region("westus2"));
        assertEquals(Collections.emptySet(), availabilityZones);
    }

    private CloudVmTypes getCloudVmTypes(Map<String, Set<String>> availabilityZones) {
        Set<VmType> vmTypes = availabilityZones.entrySet().stream().map(entry -> {
            VmTypeMeta meta = new VmTypeMeta();
            meta.setAvailabilityZones(new ArrayList(entry.getValue()));
            return VmType.vmTypeWithMeta(entry.getKey(), meta, false);
        }).collect(Collectors.toSet());
        return new CloudVmTypes(Map.of("westus2", vmTypes), null);
    }

}
