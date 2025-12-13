package com.sequenceiq.cloudbreak.service.multiaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupName;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class DataLakeAwareInstanceMetadataAvailabilityZoneCalculatorTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private AvailabilityZoneConnector availabilityZoneConnector;

    @Mock
    private InstanceMetadataAvailabilityZoneCalculator instanceMetadataAvailabilityZoneCalculator;

    @Mock
    private DetailedEnvironmentResponse environmentResponse;

    @Mock
    private EnvironmentNetworkResponse environmentNetworkResponse;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private EnvironmentService environmentClientService;

    @Spy
    @InjectMocks
    private DataLakeAwareInstanceMetadataAvailabilityZoneCalculator underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] testAvailabilityZoneDistributionForWholeInstanceGroupData() {
        return new Object[][]{
                //masterInstanceCount, auxiliaryInstanceCount, expectedZonesForGroups, expectedInstanceCountByAz
                {11,                    6,                      Set.of("1"),           Map.of("1", 17, "2", 0, "3", 0)},
                {11,                    6,                      Set.of("1", "2"),      Map.of("1", 9, "2", 8, "3", 0)},
                {11,                    6,                      Set.of("1", "2", "3"), Map.of("1", 6, "2", 6, "3", 5)},
                {2,                     1,                      Set.of("1", "2", "3"), Map.of("1", 1, "2", 1, "3", 1)},
                {1,                     2,                      Set.of("1", "2", "3"), Map.of("1", 1, "2", 1, "3", 1)},
                {1,                     1,                      Set.of("1", "2", "3"), Map.of("1", 1, "2", 1, "3", 0)},
                {0,                     7,                      Set.of("1", "2", "3"), Map.of("1", 3, "2", 2, "3", 2)},
                {56,                    0,                      Set.of("1", "2", "3"), Map.of("1", 19, "2", 19, "3", 18)},
                {5,                     4,                      Set.of("2", "3"),      Map.of("1", 0, "2", 5, "3", 4)},
                {1,                     1,                      Set.of("2", "3"),      Map.of("1", 0, "2", 1, "3", 1)},
        };
    }

    private static InstanceGroup getInstanceGroup(Set<String> environmentAvailabilityZones, long groupId, InstanceGroupName groupName, Stack stack) {
        return getInstanceGroup(environmentAvailabilityZones, groupId, groupName, stack, environmentAvailabilityZones.size());
    }

    private static InstanceGroup getInstanceGroup(Set<String> environmentAvailabilityZones, long groupId, InstanceGroupName groupName, Stack stack,
            int instanceCount) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(groupId);
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);
        instanceGroup.setGroupName(groupName.getName());
        instanceGroup.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(instanceCount, instanceGroup.getId(), instanceGroup));
        instanceGroup.setAvailabilityZones(getAvailabilityZoneSet(environmentAvailabilityZones, instanceGroup));
        instanceGroup.setStack(stack);
        return instanceGroup;
    }

    private static Set<AvailabilityZone> getAvailabilityZoneSet(Set<String> environmentAvailabilityZones, InstanceGroup ig) {
        return environmentAvailabilityZones.stream()
                .map(zone -> {
                    AvailabilityZone availabilityZone = new AvailabilityZone();
                    availabilityZone.setInstanceGroup(ig);
                    availabilityZone.setAvailabilityZone(zone);
                    return availabilityZone;
                })
                .collect(Collectors.toSet());
    }

    @Test
    void testPopulateWhenTheStackCouldNotBeFoundShouldThrowNotFoundException() {
        when(stackService.getByIdWithLists(anyLong())).thenThrow(new NotFoundException("Stack not found with id"));

        assertThrows(NotFoundException.class, () -> underTest.populate(1L));

        verify(underTest, times(0)).updateInstancesMetaData(any());
        verify(underTest, times(0)).populateSupportedOnStack(any());
        verify(underTest, times(0)).populate(any(Stack.class));
    }

    @Test
    void testPopulateWhenStackIsNotMultiAzEnabled() {
        String subnetId = "aSubnetId";
        Stack stack = TestUtil.stack();
        stack.setNetwork(TestUtil.networkWithSubnetId(subnetId));
        stack.setMultiAz(Boolean.FALSE);
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(environmentClientService.getByCrnAsInternal(ENVIRONMENT_CRN)).thenReturn(environmentResponse);

        underTest.populate(1L);

        verify(underTest, times(1)).updateInstancesMetaData(any());
        verify(underTest, times(1)).populateSupportedOnStack(stack);
        verify(underTest, times(0)).populate(stack);
        assertTrue(stack.getNotTerminatedInstanceMetaDataSet().stream()
                .allMatch(im -> StringUtils.isNotEmpty(im.getSubnetId()) && subnetId.equals(im.getSubnetId())));
    }

    @Test
    void testPopulateWhenTheStackIsMultiAzEnabledButAvailabilityZoneConnectorDoesNotExistForPlatformShouldDoNothing() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.FALSE);
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(null);
        when(environmentClientService.getByCrnAsInternal(ENVIRONMENT_CRN)).thenReturn(environmentResponse);

        underTest.populate(1L);

        verify(underTest, times(1)).updateInstancesMetaData(any());
        verify(underTest, times(1)).populateSupportedOnStack(stack);
        verify(underTest, times(0)).populate(stack);
    }

    @Test
    void testPopulateWhenTheStackIsMultiAzEnabledAndPlatformIsSupportedAndDataHub() {
        Set<String> environmentAvailabilityZones = Set.of("1", "2", "3");
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setMultiAz(Boolean.TRUE);
        stack.getInstanceGroups()
                .forEach(ig -> {
                    ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(environmentAvailabilityZones.size(), ig.getId(), ig));
                    ig.setAvailabilityZones(getAvailabilityZoneSet(environmentAvailabilityZones, ig));
                });
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        when(blueprintUtils.isEnterpriseDatalake(stack)).thenReturn(Boolean.FALSE);

        underTest.populate(1L);

        verify(underTest, times(2)).populateSupportedOnStack(stack);
        verify(underTest, times(1)).updateInstancesMetaData(stack.getNotTerminatedInstanceMetaDataSet());
        verify(underTest, times(1)).populate(stack);
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @Test
    void testPopulateWhenTheStackIsMultiAzEnabledAndPlatformIsSupportedAndNonEnterpriseDataLake() {
        Set<String> environmentAvailabilityZones = Set.of("1", "2", "3");
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setMultiAz(Boolean.TRUE);
        stack.setType(StackType.DATALAKE);
        stack.getInstanceGroups()
                .forEach(ig -> {
                    ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(environmentAvailabilityZones.size(), ig.getId(), ig));
                    ig.setAvailabilityZones(getAvailabilityZoneSet(environmentAvailabilityZones, ig));
                });
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        when(blueprintUtils.isEnterpriseDatalake(stack)).thenReturn(Boolean.FALSE);

        underTest.populate(1L);

        verify(underTest, times(2)).populateSupportedOnStack(stack);
        verify(underTest, times(1)).updateInstancesMetaData(stack.getNotTerminatedInstanceMetaDataSet());
        verify(underTest, times(1)).populate(stack);
    }

    @Test
    void testPopulateWhenMultiAzEnabledEnterpriseDataLakeHasOnlyMasterGroup() {
        String subnetId = "aSubnetId";
        Set<String> environmentAvailabilityZones = Set.of("1", "2", "3");
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setNetwork(TestUtil.networkWithSubnetId(subnetId));
        stack.setMultiAz(Boolean.TRUE);
        stack.setType(StackType.DATALAKE);
        stack.getInstanceGroups()
                .forEach(ig -> {
                    ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(environmentAvailabilityZones.size(), ig.getId(), ig));
                    ig.setAvailabilityZones(getAvailabilityZoneSet(environmentAvailabilityZones, ig));
                });
        stack.getInstanceGroups().add(getInstanceGroup(environmentAvailabilityZones, 4L, InstanceGroupName.MASTER, stack));
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        when(blueprintUtils.isEnterpriseDatalake(stack)).thenReturn(Boolean.TRUE);

        underTest.populate(1L);

        verify(underTest, times(1)).populateSupportedOnStack(stack);
        verify(underTest, times(1)).updateInstancesMetaData(stack.getNotTerminatedInstanceMetaDataSet());
        verify(underTest, times(0)).populate(stack);
        for (String expectedZone : environmentAvailabilityZones) {
            stack.getInstanceGroups().stream()
                    .filter(ig -> InstanceGroupName.MASTER.getName().equals(ig.getGroupName()))
                    .forEach(ig -> {
                        long actualInstanceCountByAz = ig.getNotDeletedInstanceMetaDataSet().stream()
                                .filter(im -> expectedZone.equals(im.getAvailabilityZone()))
                                .count();
                        assertEquals(Long.valueOf(1), actualInstanceCountByAz);
                    });
        }
        assertTrue(stack.getNotTerminatedInstanceMetaDataSet().stream()
                .allMatch(im -> StringUtils.isNotEmpty(im.getSubnetId()) && subnetId.equals(im.getSubnetId())));
    }

    @Test
    void testPopulateWhenMultiAzEnabledEnterpriseDataLakeHasOnlyAuxiliaryGroup() {
        String subnetId = "aSubnetId";
        Set<String> environmentAvailabilityZones = Set.of("1", "2", "3");
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setNetwork(TestUtil.networkWithSubnetId(subnetId));
        stack.setMultiAz(Boolean.TRUE);
        stack.setType(StackType.DATALAKE);
        stack.getInstanceGroups()
                .forEach(ig -> {
                    ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(environmentAvailabilityZones.size(), ig.getId(), ig));
                    ig.setAvailabilityZones(getAvailabilityZoneSet(environmentAvailabilityZones, ig));
                });
        stack.getInstanceGroups().add(getInstanceGroup(environmentAvailabilityZones, 4L, InstanceGroupName.AUXILIARY, stack));
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        when(blueprintUtils.isEnterpriseDatalake(stack)).thenReturn(Boolean.TRUE);

        underTest.populate(1L);

        verify(underTest, times(1)).populateSupportedOnStack(stack);
        verify(underTest, times(1)).updateInstancesMetaData(stack.getNotTerminatedInstanceMetaDataSet());
        verify(underTest, times(0)).populate(stack);
        for (String expectedZone : environmentAvailabilityZones) {
            stack.getInstanceGroups().stream()
                    .filter(ig -> InstanceGroupName.MASTER.getName().equals(ig.getGroupName()))
                    .forEach(ig -> {
                        long actualInstanceCountByAz = ig.getNotDeletedInstanceMetaDataSet().stream()
                                .filter(im -> expectedZone.equals(im.getAvailabilityZone()))
                                .count();
                        String actualRackId = ig.getNotDeletedInstanceMetaDataSet().stream()
                                .filter(im -> expectedZone.equals(im.getAvailabilityZone())).findFirst().get().getRackId();
                        assertEquals("/" + expectedZone, actualRackId);
                        assertEquals(Long.valueOf(1), actualInstanceCountByAz);
                    });
        }
        assertTrue(stack.getNotTerminatedInstanceMetaDataSet().stream()
                .allMatch(im -> StringUtils.isNotEmpty(im.getSubnetId()) && subnetId.equals(im.getSubnetId())));
    }

    @ParameterizedTest(name = "testPopulateShouldDistributeNodesAcrossInstancesOfTheMasterAndAuxiliaryGroups settings " +
            "when {0} master instances and {1} auxiliary instances and {2} expected zones for groups should result in {3} instance count by availability zone")
    @MethodSource("testAvailabilityZoneDistributionForWholeInstanceGroupData")
    void testPopulateShouldDistributeNodesAcrossInstancesOfTheMasterAndAuxiliaryGroups(int masterInstanceCount, int auxiliaryInstanceCount,
            Set<String> expectedZonesForGroups, Map<String, Integer> expectedInstanceCountByAz) {
        String subnetId = "aSubnetId";
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setNetwork(TestUtil.networkWithSubnetId(subnetId));
        stack.setMultiAz(Boolean.TRUE);
        stack.setType(StackType.DATALAKE);
        stack.getInstanceGroups()
                .forEach(ig -> {
                    ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(expectedZonesForGroups.size(), ig.getId(), ig));
                    ig.setAvailabilityZones(getAvailabilityZoneSet(expectedZonesForGroups, ig));
                });
        if (masterInstanceCount > 0) {
            stack.getInstanceGroups().add(getInstanceGroup(expectedZonesForGroups, 4L, InstanceGroupName.MASTER, stack, masterInstanceCount));
        }
        if (auxiliaryInstanceCount > 0) {
            stack.getInstanceGroups().add(getInstanceGroup(expectedZonesForGroups, 5L, InstanceGroupName.AUXILIARY, stack, auxiliaryInstanceCount));
        }
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        when(blueprintUtils.isEnterpriseDatalake(stack)).thenReturn(Boolean.TRUE);

        underTest.populate(1L);

        verify(underTest, times(1)).populateSupportedOnStack(stack);
        verify(underTest, times(1)).updateInstancesMetaData(stack.getNotTerminatedInstanceMetaDataSet());
        verify(underTest, times(0)).populate(stack);
        for (Map.Entry<String, Integer> expectedCountByAzEntry : expectedInstanceCountByAz.entrySet()) {
            String expectedZone = expectedCountByAzEntry.getKey();
            Integer expectedCountByZone = expectedCountByAzEntry.getValue();
            long actualCount = stack.getInstanceGroups().stream()
                    .filter(ig -> InstanceGroupName.MASTER.getName().equals(ig.getGroupName())
                            || InstanceGroupName.AUXILIARY.getName().equals(ig.getGroupName()))
                    .flatMap(ig -> ig.getNotTerminatedInstanceMetaDataSet().stream())
                    .filter(im -> expectedZone.equals(im.getAvailabilityZone()))
                    .count();
            assertEquals(Long.valueOf(expectedCountByZone), actualCount);
        }
        assertTrue(stack.getNotTerminatedInstanceMetaDataSet().stream()
                .allMatch(im -> StringUtils.isNotEmpty(im.getSubnetId()) && subnetId.equals(im.getSubnetId())));
    }
}