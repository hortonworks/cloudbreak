package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.ReflectionUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.MachineType;
import com.google.api.services.compute.model.MachineTypeList;
import com.google.api.services.compute.model.Network;
import com.google.api.services.compute.model.NetworkList;
import com.google.api.services.compute.model.RegionList;
import com.google.api.services.compute.model.Subnetwork;
import com.google.api.services.compute.model.SubnetworkList;
import com.google.api.services.compute.model.SubnetworkSecondaryRange;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GcpPlatformResourcesTest {

    private static final String PROJECT_ID = "project";

    private static final Region REGION = Region.region("west-us2");

    @Mock
    private ExtendedCloudCredential extendedCloudCredential;

    @Mock
    private ExtremeDiskCalculator extremeDiskCalculator;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @InjectMocks
    private GcpPlatformResources underTest;

    static Object[][] populateDataForZones() {
        return new Object[][] {
                {Map.of("west-us2-a", List.of("type-a1", "type-a2"), "west-us2-b",
                        List.of("type-b1", "type-b2"), "west-us2-c", List.of("type-c1", "type-c2")), Map.of("type-a1", Set.of("west-us2-a"),
                        "type-a2", Set.of("west-us2-a"),
                        "type-b1", Set.of("west-us2-b"), "type-b2", Set.of("west-us2-b"), "type-c1",
                        Set.of("west-us2-c"), "type-c2", Set.of("west-us2-c"))},
                {Map.of("west-us2-a", List.of("type-1", "type-2"), "west-us2-b",
                        List.of("type-1", "type-2"), "west-us2-c", List.of("type-1", "type-2")), Map.of("type-1", Set.of("west-us2-a", "west-us2-b",
                                "west-us2-c"), "type-2", Set.of("west-us2-a", "west-us2-b",
                                "west-us2-c"))},
                {Map.of("west-us2-a", List.of("type-1", "type-2"), "west-us2-b",
                        List.of(), "west-us2-c", List.of("type-1", "type-2", "type-3")), Map.of("type-1", Set.of("west-us2-a", "west-us2-c"),
                        "type-2", Set.of("west-us2-a", "west-us2-c"), "type-3", Set.of("west-us2-c"))},
                {Map.of("west-us2-a", List.of(), "west-us2-c", List.of("type-1", "type-2", "type-3")), Map.of("type-1", Set.of("west-us2-c"),
                        "type-2", Set.of("west-us2-c"), "type-3", Set.of("west-us2-c"))},
                {Map.of("west-us2-a", List.of(), "west-us2-b", List.of(), "west-us2-c", List.of()), Map.of()},
                {Map.of(), Map.of()}
        };
    }

    static Object[][] populateDataForCloudVmTypes() {
        return new Object[][] {
                {Map.of("west-us2-a", List.of("type-a1", "type-a2"), "west-us2-b",
                        List.of("type-b1", "type-b2"), "west-us2-c", List.of("type-c1", "type-c2")),
                        List.of(),
                        Map.of("type-a1", Set.of("west-us2-a"), "type-a2", Set.of("west-us2-a"), "type-b1", Set.of("west-us2-b"),
                                "type-b2", Set.of("west-us2-b"), "type-c1", Set.of("west-us2-c"), "type-c2", Set.of("west-us2-c"))},
                {Map.of("west-us2-a", List.of("type-a1", "type-a2"), "west-us2-b",
                        List.of("type-b1", "type-b2"), "west-us2-c", List.of("type-c1", "type-c2")),
                        List.of("west-us2-a"),
                        Map.of("type-a1", Set.of("west-us2-a"), "type-a2", Set.of("west-us2-a"))},
                {Map.of("west-us2-a", List.of("type-a1", "type-a2"), "west-us2-b",
                        List.of("type-a1", "type-a2"), "west-us2-c", List.of("type-c1", "type-c2")),
                        List.of("west-us2-a", "west-us2-b"),
                        Map.of("type-a1", Set.of("west-us2-a", "west-us2-b"), "type-a2", Set.of("west-us2-a", "west-us2-b"))},
                {Map.of("west-us2-a", List.of("type-a1", "type-a2"), "west-us2-b",
                        List.of("type-b1", "type-b2")),
                        List.of("us-west2-c"),
                        Map.of()},
                {Map.of("west-us2-a", List.of("type-a1", "type-a2"), "west-us2-b",
                        List.of("type-b1", "type-b2")),
                        List.of("us-west2-c", "us-west2-a"),
                        Map.of()},
                {Map.of("west-us2-a", List.of("type-a1", "type-a2"), "west-us2-b",
                        List.of("type-a1", "type-a2"), "west-us2-c", List.of("type-a1", "type-a2")),
                        List.of("west-us2-a", "west-us2-b", "west-us2-c"),
                        Map.of("type-a1", Set.of("west-us2-a", "west-us2-b", "west-us2-c"), "type-a2", Set.of("west-us2-a", "west-us2-b", "west-us2-c"))},
                {Map.of("west-us2-a", List.of("type-a1", "type-a2"), "west-us2-b",
                        List.of("type-a1", "type-a2"), "west-us2-c", List.of("type-a1", "type-a2")),
                        List.of("west-us2-a", "west-us2-b"),
                        Map.of("type-a1", Set.of("west-us2-a", "west-us2-b", "west-us2-c"), "type-a2", Set.of("west-us2-a", "west-us2-b", "west-us2-c"))},
                {Map.of(), List.of(), Map.of()}
        };
    }

    private void setUp(Map<String, List<String>> availabilityZoneToVmTypes) throws IOException {
        Field regionCoordinates = ReflectionUtils.findField(GcpPlatformResources.class, "regionCoordinates");
        ReflectionUtils.makeAccessible(regionCoordinates);
        ReflectionUtils.setField(regionCoordinates, underTest, Map.of(REGION, Coordinate.defaultCoordinate()));

        when(gcpStackUtil.getProjectId(extendedCloudCredential)).thenReturn(PROJECT_ID);

        com.google.api.services.compute.model.Region region = mock(com.google.api.services.compute.model.Region.class);
        when(region.getName()).thenReturn(REGION.getRegionName());
        when(region.getZones()).thenReturn(new ArrayList<>(availabilityZoneToVmTypes.keySet()));
        RegionList regionList = mock(RegionList.class);
        when(regionList.getItems()).thenReturn(List.of(region));
        Compute.Regions.List regionsList = mock(Compute.Regions.List.class);
        when(regionsList.execute()).thenReturn(regionList);
        Compute.Regions regions = mock(Compute.Regions.class);
        when(regions.list(PROJECT_ID)).thenReturn(regionsList);

        Compute.MachineTypes machineTypes = mock(Compute.MachineTypes.class);

        for (Map.Entry<String, List<String>> entry : availabilityZoneToVmTypes.entrySet()) {
            MachineTypeList machineTypeList = mock(MachineTypeList.class);
            List<MachineType> machineTypesForAz = entry.getValue().stream().map(machine -> {
                MachineType machineType = mock(MachineType.class);
                when(machineType.getName()).thenReturn(machine);
                return machineType;
            }).collect(Collectors.toList());
            when(machineTypeList.getItems()).thenReturn(machineTypesForAz);
            Compute.MachineTypes.List machineTypesList = mock(Compute.MachineTypes.List.class);
            when(machineTypesList.execute()).thenReturn(machineTypeList);
            when(machineTypes.list(PROJECT_ID, entry.getKey())).thenReturn(machineTypesList);
        }

        Compute compute = mock(Compute.class);
        when(compute.regions()).thenReturn(regions);
        when(compute.machineTypes()).thenReturn(machineTypes);
        when(gcpComputeFactory.buildCompute(extendedCloudCredential)).thenReturn(compute);
    }

    @ParameterizedTest(name = "testGetAvailabilityZonesForVmTypes{index}")
    @MethodSource("populateDataForZones")
    public void testGetAvailabilityZonesForVmTypes(Map<String, List<String>> availabilityZoneToVmTypes, Map<String, Set<String>> expectedResult)
            throws IOException {
        setUp(availabilityZoneToVmTypes);

        Map<String, Set<String>> result = underTest.getAvailabilityZonesForVmTypes(extendedCloudCredential, REGION);

        assertEquals(expectedResult, result);
    }

    @ParameterizedTest(name = "testVirtualMachines{index}")
    @MethodSource("populateDataForCloudVmTypes")
    public void testVirtualMachines(Map<String, List<String>> availabilityZoneToVmTypes, List<String> availabilityZones, Map<String, Set<String>> expectedVms)
            throws IOException {
        setUp(availabilityZoneToVmTypes);

        CloudVmTypes cloudVmTypes = underTest.virtualMachines(extendedCloudCredential, REGION,
                availabilityZones != null ? Map.of(NetworkConstants.AVAILABILITY_ZONES, String.join(",", availabilityZones)) : null);

        assertEquals(availabilityZoneToVmTypes.size(), cloudVmTypes.getCloudVmResponses().size());
        for (Set<VmType> vmTypes : cloudVmTypes.getCloudVmResponses().values()) {
            assertEquals(expectedVms.size(), vmTypes.size());
            expectedVms.entrySet().stream().forEach(entry -> {
                assertTrue(vmTypes.stream().anyMatch(vmType -> vmType.getValue().equals(entry.getKey())));
                assertTrue(vmTypes.stream().anyMatch(vmType -> vmType.getMetaData().getBalancedHddConfig() != null));
                assertEquals(entry.getValue(), new HashSet<>(vmTypes.stream().filter(vmType ->
                        vmType.getValue().equals(entry.getKey())).findFirst().orElse(null).getMetaData().getAvailabilityZones()));
            });
        }
    }

    @Test
    void testNetworks() throws Exception {
        Compute compute = mock(Compute.class);
        Compute.Networks networks = mock(Compute.Networks.class);
        Network network = mock(Network.class);
        Compute.Networks.List list = mock(Compute.Networks.List.class);
        NetworkList networkList = mock(NetworkList.class);
        Compute.Networks.Get get = mock(Compute.Networks.Get.class);
        Compute.Subnetworks subnetworks = mock(Compute.Subnetworks.class);
        Compute.Subnetworks.List subnetworkList = mock(Compute.Subnetworks.List.class);
        SubnetworkList subnetworkList1 = mock(SubnetworkList.class);
        Compute.Subnetworks.Get subnetwork = mock(Compute.Subnetworks.Get.class);
        Compute.Regions regions = mock(Compute.Regions.class);
        Compute.Regions.Get regionsGet = mock(Compute.Regions.Get.class);
        com.google.api.services.compute.model.Region region = mock(com.google.api.services.compute.model.Region.class);
        Region region1 = mock(Region.class);
        Subnetwork subnetwork1 = mock(Subnetwork.class);
        SubnetworkSecondaryRange range1 = mock(SubnetworkSecondaryRange.class);
        SubnetworkSecondaryRange range2 = mock(SubnetworkSecondaryRange.class);

        when(region1.value()).thenReturn("region1");
        when(network.getId()).thenReturn(new BigInteger("1"));
        when(range1.getIpCidrRange()).thenReturn("cidr1");
        when(range2.getIpCidrRange()).thenReturn("cidr2");
        when(range1.getRangeName()).thenReturn("range1");
        when(range2.getRangeName()).thenReturn("range2");
        when(network.getGatewayIPv4()).thenReturn("10.0.0.0/16");
        when(network.getDescription()).thenReturn("description");
        when(network.getIPv4Range()).thenReturn("description");
        when(network.getCreationTimestamp()).thenReturn("123");
        when(network.getSubnetworks()).thenReturn(List.of("s1"));
        when(subnetwork1.getSelfLink()).thenReturn("s1");
        when(subnetwork1.getId()).thenReturn(new BigInteger("1"));
        when(subnetwork1.getName()).thenReturn("s1name");
        when(subnetwork1.getIpCidrRange()).thenReturn("1.1.1.1");
        when(subnetwork1.getPrivateIpGoogleAccess()).thenReturn(true);
        when(subnetwork1.getPrivateIpGoogleAccess()).thenReturn(true);
        when(subnetwork1.getSecondaryIpRanges()).thenReturn(List.of(range1, range2));
        when(subnetworkList1.getItems()).thenReturn(List.of(subnetwork1));
        when(region.getZones()).thenReturn(List.of("zone1"));
        when(regionsGet.execute()).thenReturn(region);
        when(regions.get(any(), any())).thenReturn(regionsGet);
        when(compute.regions()).thenReturn(regions);
        when(gcpComputeFactory.buildCompute(any())).thenReturn(compute);
        when(gcpStackUtil.getProjectId(any())).thenReturn("id");
        when(networkList.getItems()).thenReturn(List.of(network));
        when(networks.get(any(), any())).thenReturn(get);
        when(list.execute()).thenReturn(networkList);
        when(networks.list(any())).thenReturn(list);
        when(compute.networks()).thenReturn(networks);
        when(subnetworkList.execute()).thenReturn(subnetworkList1);
        when(subnetworks.list(any(), any())).thenReturn(subnetworkList);
        when(compute.subnetworks()).thenReturn(subnetworks);
        when(subnetworks.list(any(), any())).thenReturn(subnetworkList);
        when(subnetworks.get(any(), any(), any())).thenReturn(subnetwork);
        when(compute.subnetworks()).thenReturn(subnetworks);

        CloudNetworks result = underTest.networks(extendedCloudCredential, region1, Map.of());

        assertEquals(result.getCloudNetworkResponses().keySet(), Set.of("region1"));
        Set<CloudSubnet> cloudSubnets = result.getCloudNetworkResponses().get("region1")
                .stream()
                .findFirst()
                .get()
                .getSubnetsMeta();
        CloudSubnet cloudSubnet = cloudSubnets
                .stream()
                .findFirst()
                .get();

        assertThat(cloudSubnet.getSecondaryCidrs(), contains("cidr1", "cidr2"));
        assertThat(cloudSubnet.getSecondaryCidrsWithNames().entrySet(), contains(
                Map.entry("cidr1", "range1"),
                Map.entry("cidr2", "range2")
        ));
    }
}
