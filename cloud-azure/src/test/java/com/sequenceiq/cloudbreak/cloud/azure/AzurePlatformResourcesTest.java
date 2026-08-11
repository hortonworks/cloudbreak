package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.resourcemanager.compute.models.VirtualMachineSize;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.azure.resourcemanager.network.fluent.models.SubnetInner;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.Subnet;
import com.azure.resourcemanager.network.models.VirtualNetworkPrivateEndpointNetworkPolicies;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRegionProvider;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureAcceleratedNetworkValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureHostEncryptionValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.constant.AzureConstants;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
class AzurePlatformResourcesTest {

    private static final String REGION = "eastus";

    private static final int NO_RESOURCE_DISK_ATTACHED_TO_INSTANCE = 0;

    private static final String ARM_VM_DEFAULT = "Standard_D4s_v5";

    private static final int MAX_DISK_SIZE = 2048;

    @Mock
    private AzureClient azureClient;

    @Mock
    private ExtendedCloudCredential cloudCredential;

    @Mock
    private AzureRegionProvider azureRegionProvider;

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureAddressPrefixProvider azureAddressPrefixProvider;

    @Mock
    private AzureCloudSubnetParametersService azureCloudSubnetParametersService;

    @Mock
    private AzureHostEncryptionValidator azureHostEncryptionValidator;

    @Mock
    private AzureAcceleratedNetworkValidator azureAcceleratedNetworkValidator;

    @InjectMocks
    private AzurePlatformResources underTest;

    @Test
    void testVirtualMachinesWhenNoInstanceTypeShouldBeFilteredOut() {
        Region region = region("westeruope");
        Set<VirtualMachineSize> virtualMachineSizes = new HashSet<>();
        virtualMachineSizes.add(createVirtualMachineSize("Standard_D2s_v5", 20000));
        virtualMachineSizes.add(createVirtualMachineSize("Standard_E64ds_v5", 1400000));
        when(azureClient.getVmTypes(region.value())).thenReturn(Optional.of(virtualMachineSizes));
        when(azureClient.getAvailabilityZones(region.value())).thenReturn(Map.of());
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureAcceleratedNetworkValidator.isSupportedForVm(any(), anyMap())).thenReturn(true);

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
        assertTrue(actual.getCloudVmResponses().get(region.value()).stream()
                .allMatch(vmType -> (boolean) vmType.getMetaData().getProperties().get("EnhancedNetwork")));
        assertTrue(actual.getCloudVmResponses().get(region.value()).stream()
                .noneMatch(vmType -> vmType.getMetaData().getHostEncryptionSupported()));
    }

    @Test
    void testVirtualMachinesWhenInstanceTypeHasZeroResourceDiskMustBePresented() {
        Region region = region("westeruope");
        Set<VirtualMachineSize> virtualMachineSizes = new HashSet<>();
        VirtualMachineSize d2sTypeWithoutResourceDisk = createVirtualMachineSize("Standard_D2s_v5", 0);
        VirtualMachineSize e64sVmTypeWithoutResourceDisk = createVirtualMachineSize("Standard_E64s_v5", 0);
        virtualMachineSizes.add(createVirtualMachineSize(ARM_VM_DEFAULT, 20000));
        virtualMachineSizes.add(d2sTypeWithoutResourceDisk);
        virtualMachineSizes.add(createVirtualMachineSize("Standard_E64ds_v5", 1400000));
        virtualMachineSizes.add(e64sVmTypeWithoutResourceDisk);
        when(azureClient.getVmTypes(region.value())).thenReturn(Optional.of(virtualMachineSizes));
        Map<String, List<String>> zoneInfo = new HashMap<>();
        zoneInfo.put(ARM_VM_DEFAULT, List.of("1", "2"));
        zoneInfo.put("Standard_E64s_v5", List.of("2", "3"));
        zoneInfo.put("Standard_D2s_v5", List.of("1"));
        zoneInfo.put("Standard_E64ds_v5", List.of("1", "2", "3"));
        when(azureClient.getAvailabilityZones(region.value())).thenReturn(zoneInfo);
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureHostEncryptionValidator.isVmSupported(any(), anyMap())).thenReturn(true);

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
        assertTrue(actual.getCloudVmResponses().get(region.value()).stream()
                .noneMatch(vmType -> (boolean) vmType.getMetaData().getProperties().get("EnhancedNetwork")));
        assertTrue(actual.getCloudVmResponses().get(region.value()).stream()
                .allMatch(vmType -> vmType.getMetaData().getHostEncryptionSupported()));
    }

    static Object []  [] dataForFilterVirtualMachines() {
        return new Object [][] {
                {Map.of("Standard_D2s_v5", List.of("1", "2"), "Standard_E64s_v5", List.of(), ARM_VM_DEFAULT, List.of("1"),
                        "Standard_E64ds_v5", List.of("1", "2", "3")),
                        List.of("1"),
                        Set.of("Standard_D2s_v5", ARM_VM_DEFAULT, "Standard_E64ds_v5")},
                {Map.of("Standard_D2s_v5", List.of("1", "2"), "Standard_E64s_v5", List.of("2", "3"), ARM_VM_DEFAULT, List.of("1"),
                        "Standard_E64ds_v5", List.of("1", "2", "3")),
                        List.of("3", "1", "2"),
                        Set.of("Standard_E64ds_v5")},
                {Map.of("Standard_D2s_v5", List.of("1", "2"), "Standard_E64s_v5", List.of("1", "2"), ARM_VM_DEFAULT, List.of("1"),
                        "Standard_E64ds_v5", List.of("1", "2", "3")),
                        List.of("1", "2"),
                        Set.of("Standard_D2s_v5", "Standard_E64s_v5", "Standard_E64ds_v5")},
                {Map.of("Standard_D2s_v5", List.of("1", "2"), "Standard_E64s_v5", List.of("1", "2"), ARM_VM_DEFAULT, List.of("1"),
                        "Standard_E64ds_v5", List.of("1", "2", "3")),
                        null,
                        Set.of("Standard_D2s_v5", "Standard_E64s_v5", ARM_VM_DEFAULT, "Standard_E64ds_v5")},
                {Map.of("Standard_D2s_v5", List.of("1", "2"), "Standard_E64s_v5", List.of("1", "2"), ARM_VM_DEFAULT, List.of("1"),
                        "Standard_E64ds_v5", List.of("1", "2", "3")),
                        List.of(),
                        Set.of("Standard_D2s_v5", "Standard_E64s_v5", ARM_VM_DEFAULT, "Standard_E64ds_v5")},
                {Map.of("Standard_D2s_v5", List.of("1", "2"), "Standard_E64s_v5", List.of("1", "2"), ARM_VM_DEFAULT, List.of("1"),
                        "Standard_E64ds_v5", List.of("1", "2", "3")),
                        List.of("4"),
                        Set.of()}
        };
    }

    @ParameterizedTest
    @MethodSource("dataForFilterVirtualMachines")
    void testVirtualMachinesForFilters(Map<String, List<String>> zoneInfo, List<String> availabilityZones, Set<String> expectedVirtualMachines) {
        Region region = region("westus2");

        Set<VirtualMachineSize> virtualMachineSizes = zoneInfo.keySet().stream().map(vname -> createVirtualMachineSize(vname, 0))
                .collect(Collectors.toSet());

        when(azureClient.getVmTypes(region.value())).thenReturn(Optional.of(virtualMachineSizes));
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getAvailabilityZones(region.value())).thenReturn(zoneInfo);

        CloudVmTypes actual = underTest.virtualMachines(cloudCredential, region, availabilityZones != null ? Map.of(NetworkConstants.AVAILABILITY_ZONES,
                String.join(",", availabilityZones)) : null);

        assertNotNull(actual);
        assertNotNull(actual.getCloudVmResponses());
        Set<VmType> vmTypes = actual.getCloudVmResponses().get(region.value());
        assertNotNull(vmTypes);
        assertEquals(expectedVirtualMachines.size(), vmTypes.size());
        expectedVirtualMachines.forEach(virtualMachineSize -> {
            VmType matchingVm = vmTypes
                    .stream()
                    .filter(vmType -> virtualMachineSize.equals(vmType.value()))
                    .findFirst()
                    .orElse(null);
            if (matchingVm == null) {
                fail("VM from input is not present in response");
            }
            assertEquals(zoneInfo.get(matchingVm.value()), matchingVm.getMetaData().getAvailabilityZones());
        });
    }

    @Test
    void testVirtualMachinesForFiltersAzForInstanceIsNull() {
        Map<String, List<String>> zoneInfo = new HashMap<>();
        zoneInfo.put("Standard_D2s_v5", List.of("1", "2"));
        zoneInfo.put(ARM_VM_DEFAULT, List.of("1"));
        zoneInfo.put("Standard_E64ds_v5", List.of("1", "2", "3"));
        zoneInfo.put("Standard_E64s_v5", null);
        List<String> availabilityZones = List.of("1");
        Set<String> expectedVirtualMachines = Set.of("Standard_D2s_v5", ARM_VM_DEFAULT, "Standard_E64ds_v5");
        Region region = region("westus2");

        Set<VirtualMachineSize> virtualMachineSizes = zoneInfo.keySet().stream().map(vname -> createVirtualMachineSize(vname, 0))
                .collect(Collectors.toSet());

        when(azureClient.getVmTypes(region.value())).thenReturn(Optional.of(virtualMachineSizes));
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getAvailabilityZones(region.value())).thenReturn(zoneInfo);
        when(azureHostEncryptionValidator.isVmSupported(any(), anyMap())).thenReturn(true);

        CloudVmTypes actual = underTest.virtualMachines(cloudCredential, region, availabilityZones != null ? Map.of(NetworkConstants.AVAILABILITY_ZONES,
                String.join(",", availabilityZones)) : null);

        assertNotNull(actual);
        assertNotNull(actual.getCloudVmResponses());
        Set<VmType> vmTypes = actual.getCloudVmResponses().get(region.value());
        assertNotNull(vmTypes);
        assertEquals(expectedVirtualMachines.size(), vmTypes.size());
        expectedVirtualMachines.forEach(virtualMachineSize -> {
            VmType matchingVm = vmTypes
                    .stream()
                    .filter(vmType -> virtualMachineSize.equals(vmType.value()))
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
        Region region = region("us-west-1");
        CloudContext cloudContext = new CloudContext.Builder()
                .withLocation(Location.location(region, availabilityZone("us-west-1")))
                .build();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        Set<VirtualMachineSize> virtualMachineSizes = new HashSet<>();
        virtualMachineSizes.add(createVirtualMachineSize("Standard_D8_v5", 20000));
        when(azureClient.getVmTypes(region.value())).thenReturn(Optional.of(virtualMachineSizes));
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        InstanceStoreMetadata instanceStoreMetadata = underTest.collectInstanceStorageCount(ac, Collections.singletonList("Standard_D8_v5"));

        assertEquals(1, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("Standard_D8_v5"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("unsupported"));
    }

    @Test
    void collectInstanceStorageCountWhenInstanceTypeIsNotFoundTest() {
        Region region = region("us-west-1");
        CloudContext cloudContext = new CloudContext.Builder()
                .withLocation(Location.location(region, availabilityZone("us-west-1")))
                .build();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        Set<VirtualMachineSize> virtualMachineSizes = new HashSet<>();
        when(azureClient.getVmTypes(region.value())).thenReturn(Optional.of(virtualMachineSizes));
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        InstanceStoreMetadata instanceStoreMetadata = underTest.collectInstanceStorageCount(ac, Collections.singletonList("unsupported"));

        assertNull(instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("unsupported"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("unsupported"));
        assertNull(instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("Standard_D8_v5"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("Standard_D8_v5"));

        when(azureClient.getVmTypes(region.value())).thenReturn(Optional.of(virtualMachineSizes));

        instanceStoreMetadata = underTest.collectInstanceStorageCount(ac, new ArrayList<>());

        assertNull(instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("Standard_D8_v5"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("Standard_D8_v5"));
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

    @Test
    void testNetworksWhenNoNetworks() {
        Region region = region("westus2");
        AzureListResult<Network> azureListResult = mock(AzureListResult.class);
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getNetworks()).thenReturn(azureListResult);
        CloudNetworks networks = underTest.networks(cloudCredential, region, Map.of());
        assertTrue(networks.getCloudNetworkResponses().get("westus2").isEmpty());
    }

    @Test
    void testNetworksWhenNetworkWithoutRegion() {
        Region region = region("westus2");
        AzureListResult<Network> azureListResult = mock(AzureListResult.class);
        Network network = mock(Network.class);
        when(azureListResult.getAll()).thenReturn(List.of(network));
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getNetworks()).thenReturn(azureListResult);
        assertThrows(BadRequestException.class, () -> underTest.networks(cloudCredential, region, Map.of()));
    }

    @Test
    void testNetworksWhenNetworkWithRegion() {
        Region region = region("westus2");
        AzureListResult<Network> azureListResult = mock(AzureListResult.class);
        Network network = mock(Network.class);
        when(network.region()).thenReturn(com.azure.core.management.Region.US_WEST2);
        when(azureListResult.getAll()).thenReturn(List.of(network));
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getNetworks()).thenReturn(azureListResult);
        CloudNetworks actual = underTest.networks(cloudCredential, region, Map.of());
        assertFalse(actual.getCloudNetworkResponses().get("westus2").isEmpty());
    }

    @Test
    void testNetworksWhenNetworkWhenResourceGroupAndNetworkIdGiven() {
        Region region = region("westus2");
        Map<String, String> filter = Map.ofEntries(
                Map.entry(AzureConstants.NETWORK_ID, "networkid"),
                Map.entry(AzureConstants.RESOURCE_GROUP_NAME, "resourcegroup"));
        Network network = mock(Network.class);
        when(network.region()).thenReturn(com.azure.core.management.Region.US_WEST2);
        List<String> addressSpaces = List.of("as1", "as2");
        List<String> dnsServerIPs = List.of("dns1", "dns2");
        when(network.addressSpaces()).thenReturn(addressSpaces);
        when(network.dnsServerIPs()).thenReturn(dnsServerIPs);
        when(network.resourceGroupName()).thenReturn("rg");
        Subnet subnet = mock(Subnet.class);
        when(subnet.innerModel()).thenReturn(new SubnetInner()
                .withDelegations(List.of())
                .withPrivateEndpointNetworkPolicies(VirtualNetworkPrivateEndpointNetworkPolicies.fromString("")));
        when(network.subnets()).thenReturn(Map.of("subnet", subnet));
        when(azureAddressPrefixProvider.getAddressPrefix(any())).thenReturn("10.0.0.0/16");
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getNetworkByResourceGroup("resourcegroup", "networkid")).thenReturn(Optional.of(network));
        CloudNetworks actualNetworks = underTest.networks(cloudCredential, region, filter);
        assertFalse(actualNetworks.getCloudNetworkResponses().get("westus2").isEmpty());
        verify(azureCloudSubnetParametersService).addPrivateEndpointNetworkPolicies(
                any(CloudSubnet.class), any(VirtualNetworkPrivateEndpointNetworkPolicies.class));
        verify(azureCloudSubnetParametersService).addFlexibleServerDelegatedSubnet(any(CloudSubnet.class), anyList());
        CloudNetwork actualNetwork = actualNetworks.getCloudNetworkResponses().get("westus2").iterator().next();
        assertEquals(addressSpaces, actualNetwork.getProperties().get("addressSpaces"));
        assertEquals(dnsServerIPs, actualNetwork.getProperties().get("dnsServerIPs"));
        assertEquals("rg", actualNetwork.getProperties().get("resourceGroupName"));
        assertEquals("subnet", actualNetwork.getSubnetsMeta().iterator().next().getId());
    }

    @Test
    void testGetCloudVmTypesKeepsDeprecatedAndGen2OnlyFilteringStillApplies() {
        ReflectionTestUtils.setField(underTest, "armVmDefault", ARM_VM_DEFAULT);

        when(azureClientService.getClient(any())).thenReturn(azureClient);

        // ARM_VM_DEFAULT = Standard_D4s_v5 — not deprecated, not gen2-only → kept, becomes default
        // Standard_D2_v2 — deprecated (Dsv2 family), kept in cloudVmResponses and tracked in deprecatedCloudVmResponses
        // Standard_D2_v6 — not deprecated, but gen2-only (_v6 suffix) → removed
        // Standard_D4s_v7 — not deprecated, but gen2-only (_v7 suffix) → removed
        // Basic_A0 — no version suffix, not deprecated → kept
        // Standard_D1 — deprecated (D family without version), kept in cloudVmResponses and tracked in deprecatedCloudVmResponses
        // Standard_E16s_v5 — not deprecated, not gen2-only → kept
        VirtualMachineSize vmA = mockVmSize(ARM_VM_DEFAULT);
        VirtualMachineSize vmB = mockVmSize("Standard_D2_v2");
        VirtualMachineSize vmC = mockVmSize("Standard_D2_v6");
        VirtualMachineSize vmD = mockVmSize("Standard_D4s_v7");
        VirtualMachineSize vmE = mockVmSize("Basic_A0");
        VirtualMachineSize vmF = mockVmSize("Standard_D1");
        VirtualMachineSize vmG = mockVmSize("Standard_E16s_v5");
        Set<VirtualMachineSize> vmSizes = new HashSet<>(Arrays.asList(vmA, vmB, vmC, vmD, vmE, vmF, vmG));
        when(azureClient.getVmTypes(REGION)).thenReturn(Optional.of(vmSizes));

        Map<String, List<String>> azs = new HashMap<>();
        azs.put(ARM_VM_DEFAULT, Arrays.asList("1", "2"));
        azs.put("Standard_D2_v2", Arrays.asList("1", "3"));
        azs.put("Standard_D2_v6", Arrays.asList("1", "2"));
        azs.put("Standard_D4s_v7", Arrays.asList("1", "2"));
        azs.put("Basic_A0", Arrays.asList("1", "2"));
        azs.put("Standard_D1", Arrays.asList("1", "2"));
        azs.put("Standard_E16s_v5", Arrays.asList("1", "2"));
        when(azureClient.getAvailabilityZones(REGION)).thenReturn(azs);

        AzureVmCapabilities gen2OnlyCaps = mock(AzureVmCapabilities.class);
        when(gen2OnlyCaps.isGen1Supported()).thenReturn(false);
        Map<String, AzureVmCapabilities> capabilities = new HashMap<>();
        capabilities.put(ARM_VM_DEFAULT, gen1SupportedCapabilities());
        capabilities.put("Basic_A0", gen1SupportedCapabilities());
        capabilities.put("Standard_E16s_v5", gen1SupportedCapabilities());
        capabilities.put("Standard_D2_v6", gen2OnlyCaps);
        capabilities.put("Standard_D4s_v7", gen2OnlyCaps);
        when(azureClient.getHostCapabilities(REGION)).thenReturn(capabilities);
        when(azureHostEncryptionValidator.isVmSupported(anyString(), anyMap())).thenReturn(true);
        when(azureAcceleratedNetworkValidator.isSupportedForVm(anyString(), anyMap())).thenReturn(true);

        CloudVmTypes result = underTest.virtualMachinesNonExtended(cloudCredential, region(REGION), Map.of());

        Set<VmType> returnedTypes = result.getCloudVmResponses().get(REGION);
        Set<VmType> deprecatedTypes = result.getDeprecatedCloudVmResponses().get(REGION);
        VmType defaultType = result.getDefaultCloudVmResponses().get(REGION);
        assertThat(returnedTypes)
                .extracting(VmType::value)
                .containsExactlyInAnyOrder(ARM_VM_DEFAULT, "Basic_A0", "Standard_E16s_v5", "Standard_D2_v2", "Standard_D1");
        assertThat(deprecatedTypes)
                .extracting(VmType::value)
                .containsExactlyInAnyOrder("Standard_D2_v2", "Standard_D1");
        assertThat(defaultType.value()).isEqualTo(ARM_VM_DEFAULT);
    }

    @Test
    void testVirtualMachinesNonExtendedPopulatesArchitectureFromCapabilities() {
        ReflectionTestUtils.setField(underTest, "armVmDefault", "Standard_E4pds_v5");

        when(azureClientService.getClient(any())).thenReturn(azureClient);

        VirtualMachineSize armVm = mockVmSize("Standard_E4pds_v5");
        VirtualMachineSize x64Vm = mockVmSize("Standard_D8s_v5");
        VirtualMachineSize noCapabilityVm = mockVmSize("Standard_D2s_v5");
        Set<VirtualMachineSize> vmSizes = new HashSet<>(Arrays.asList(armVm, x64Vm, noCapabilityVm));
        when(azureClient.getVmTypes(REGION)).thenReturn(Optional.of(vmSizes));

        Map<String, List<String>> azs = new HashMap<>();
        azs.put("Standard_E4pds_v5", Arrays.asList("1", "2"));
        azs.put("Standard_D8s_v5", Arrays.asList("1", "2"));
        azs.put("Standard_D2s_v5", Arrays.asList("1", "2"));
        when(azureClient.getAvailabilityZones(REGION)).thenReturn(azs);

        AzureVmCapabilities armCapability = mock(AzureVmCapabilities.class);
        when(armCapability.getArchitecture()).thenReturn(Architecture.ARM64);
        when(armCapability.isGen1Supported()).thenReturn(true);
        AzureVmCapabilities x64Capability = mock(AzureVmCapabilities.class);
        when(x64Capability.getArchitecture()).thenReturn(Architecture.X86_64);
        when(x64Capability.isGen1Supported()).thenReturn(true);
        Map<String, AzureVmCapabilities> capabilities = new HashMap<>();
        capabilities.put("Standard_E4pds_v5", armCapability);
        capabilities.put("Standard_D8s_v5", x64Capability);
        // Standard_D2s_v5 intentionally has no capability entry -> should fall back to X86_64.
        when(azureClient.getHostCapabilities(REGION)).thenReturn(capabilities);
        when(azureHostEncryptionValidator.isVmSupported(anyString(), anyMap())).thenReturn(true);
        when(azureAcceleratedNetworkValidator.isSupportedForVm(anyString(), anyMap())).thenReturn(true);

        CloudVmTypes result = underTest.virtualMachinesNonExtended(cloudCredential, region(REGION), Map.of());

        Map<String, Architecture> architectureByVmType = result.getCloudVmResponses().get(REGION).stream()
                .collect(Collectors.toMap(VmType::value, vmType -> vmType.getMetaData().getArchitecture()));
        assertThat(architectureByVmType)
                .containsEntry("Standard_E4pds_v5", Architecture.ARM64)
                .containsEntry("Standard_D8s_v5", Architecture.X86_64)
                .containsEntry("Standard_D2s_v5", Architecture.X86_64);
    }

    static Stream<String> deprecatedFamilyNames() {
        return Stream.of(
                // Compute Optimized F-series
                "Standard_F4", "Standard_F8s", "Standard_F4s_v2",
                // General Purpose D v1 and Dv2 only (v3/v4 are NOT capacity-restricted)
                "Standard_D2", "Standard_DS4", "Standard_D2_v2", "Standard_DS2_v2",
                // General Purpose A-series
                "Standard_A2_v2", "Standard_A4m_v2",
                // General Purpose B-series
                "Standard_B2s", "Standard_B4ms", "Standard_B8ms",
                // Memory Optimized G-series
                "Standard_G1", "Standard_GS2",
                // Storage Optimized L-series
                "Standard_L8s", "Standard_L8s_v2"
        );
    }

    @ParameterizedTest
    @MethodSource("deprecatedFamilyNames")
    void testDeprecatedFamilyIsMarkedDeprecated(String vmName) {
        Boolean deprecated = ReflectionTestUtils.invokeMethod(underTest, "isDeprecatedVmType", mockVmSize(vmName));
        assertThat(deprecated)
                .as("Expected %s to be marked deprecated", vmName)
                .isTrue();
    }

    static Stream<String> nonDeprecatedFamilyNames() {
        return Stream.of(
                // D-series v3/v4 — NOT in the capacity restriction list
                "Standard_D4_v3", "Standard_D4s_v3", "Standard_D4_v4", "Standard_D4s_v4",
                "Standard_D4ds_v4", "Standard_D4d_v4",
                // D-series v5+
                "Standard_D4s_v5", "Standard_D2_v6",
                // E-series (not deprecated)
                "Standard_E8s_v5", "Standard_E16s_v6",
                // L-series v3+ (not deprecated)
                "Standard_L8s_v3", "Standard_L16s_v4", "Standard_L8as_v3", "Standard_L16as_v3",
                // FX-series (not deprecated)
                "Standard_FX4mds"
        );
    }

    @ParameterizedTest
    @MethodSource("nonDeprecatedFamilyNames")
    void testNonDeprecatedFamilyIsNotMarkedDeprecated(String vmName) {
        Boolean deprecated = ReflectionTestUtils.invokeMethod(underTest, "isDeprecatedVmType", mockVmSize(vmName));
        assertThat(deprecated)
                .as("Expected %s to NOT be marked deprecated", vmName)
                .isFalse();
    }

    @Test
    void testGen1UnsupportedFilterRemovesVm() {
        AzureVmCapabilities gen2OnlyCaps = mock(AzureVmCapabilities.class);
        when(gen2OnlyCaps.isGen1Supported()).thenReturn(false);
        Map<String, AzureVmCapabilities> caps = Map.of("Standard_D2_v6", gen2OnlyCaps);

        Predicate<VirtualMachineSize> filter = ReflectionTestUtils.invokeMethod(underTest, "filterOutGen1UnsupportedVms", caps);

        assertThat(filter.test(mockVmSize("Standard_D2_v6"))).isFalse();
    }

    @Test
    void testGen1UnsupportedFilterKeepsVmWhenGen1Supported() {
        AzureVmCapabilities gen1AndGen2Caps = mock(AzureVmCapabilities.class);
        when(gen1AndGen2Caps.isGen1Supported()).thenReturn(true);
        Map<String, AzureVmCapabilities> caps = Map.of("Standard_D4s_v5", gen1AndGen2Caps);

        Predicate<VirtualMachineSize> filter = ReflectionTestUtils.invokeMethod(underTest, "filterOutGen1UnsupportedVms", caps);

        assertThat(filter.test(mockVmSize("Standard_D4s_v5"))).isTrue();
    }

    @Test
    void testGen1UnsupportedFilterKeepsVmWhenCapabilitiesAbsent() {
        Predicate<VirtualMachineSize> filter = ReflectionTestUtils.invokeMethod(underTest, "filterOutGen1UnsupportedVms", Map.of());

        assertThat(filter.test(mockVmSize("Standard_D4s_v5"))).isTrue();
    }

    private AzureVmCapabilities gen1SupportedCapabilities() {
        AzureVmCapabilities caps = mock(AzureVmCapabilities.class);
        when(caps.isGen1Supported()).thenReturn(true);
        return caps;
    }

    private VirtualMachineSize mockVmSize(String name) {
        VirtualMachineSize vm = mock(VirtualMachineSize.class);
        when(vm.name()).thenReturn(name);
        return vm;
    }
}
