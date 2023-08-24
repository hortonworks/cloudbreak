package com.sequenceiq.cloudbreak.service.multiaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupName;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class DataLakeAwareInstanceMetadataAvailabilityZoneCalculatorTest {

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
    private MultiAzCalculatorService multiAzCalculatorService;

    @Spy
    @InjectMocks
    private DataLakeAwareInstanceMetadataAvailabilityZoneCalculator underTest;

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
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.FALSE);
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);

        underTest.populate(1L);

        verify(underTest, times(0)).updateInstancesMetaData(any());
        verify(underTest, times(1)).populateSupportedOnStack(stack);
        verify(underTest, times(0)).populate(stack);
    }

    @Test
    void testPopulateWhenTheStackIsMultiAzEnabledButAvailabilityZoneConnectorDoesNotExistForPlatformShouldDoNothing() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.FALSE);
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(null);

        underTest.populate(1L);

        verify(underTest, times(0)).updateInstancesMetaData(any());
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
    void testPopulateWhenMultiAzEnabledEnterpriseDataLakeHasOnlyGatewayGroup() {
        Set<String> environmentAvailabilityZones = Set.of("1", "2", "3");
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setMultiAz(Boolean.TRUE);
        stack.setType(StackType.DATALAKE);
        stack.getInstanceGroups()
                .forEach(ig -> {
                    ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(environmentAvailabilityZones.size(), ig.getId(), ig));
                    ig.setAvailabilityZones(getAvailabilityZoneSet(environmentAvailabilityZones, ig));
                });
        stack.getInstanceGroups().add(getInstanceGroup(environmentAvailabilityZones, 4L, InstanceGroupName.GATEWAY, stack));
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        when(blueprintUtils.isEnterpriseDatalake(stack)).thenReturn(Boolean.TRUE);

        underTest.populate(1L);

        verify(underTest, times(1)).populateSupportedOnStack(stack);
        verify(underTest, times(1)).updateInstancesMetaData(stack.getNotTerminatedInstanceMetaDataSet());
        verify(underTest, times(0)).populate(stack);
        for (String expectedZone : environmentAvailabilityZones) {
            stack.getInstanceGroups().stream()
                    .filter(ig -> InstanceGroupName.GATEWAY.getName().equals(ig.getGroupName()))
                    .forEach(ig -> {
                        long actualInstanceCountByAz = ig.getNotDeletedInstanceMetaDataSet().stream()
                                .filter(im -> expectedZone.equals(im.getAvailabilityZone()))
                                .count();
                        assertEquals(Long.valueOf(1), actualInstanceCountByAz);
                    });
        }
    }

    @Test
    void testPopulateWhenMultiAzEnabledEnterpriseDataLakeHasOnlyAuxiliaryGroup() {
        Set<String> environmentAvailabilityZones = Set.of("1", "2", "3");
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
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
        when(multiAzCalculatorService.determineRackId(any(), eq("1"))).thenReturn("/1");
        when(multiAzCalculatorService.determineRackId(any(), eq("2"))).thenReturn("/2");
        when(multiAzCalculatorService.determineRackId(any(), eq("3"))).thenReturn("/3");

        underTest.populate(1L);

        verify(underTest, times(1)).populateSupportedOnStack(stack);
        verify(underTest, times(1)).updateInstancesMetaData(stack.getNotTerminatedInstanceMetaDataSet());
        verify(underTest, times(0)).populate(stack);
        for (String expectedZone : environmentAvailabilityZones) {
            stack.getInstanceGroups().stream()
                    .filter(ig -> InstanceGroupName.GATEWAY.getName().equals(ig.getGroupName()))
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
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] testAvailabilityZoneDistributionForWholeInstanceGroupData() {
        return new Object[][]{
                //gatewayInstanceCount, auxiliaryInstanceCount, expectedZonesForGroups, expectedInstanceCountByAz
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
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "testPopulateShouldDistributeNodesAcrossInstancesOfTheGatewayAndAuxiliaryGroups settings " +
            "when {0} gateway instances and {1} auxiliary instances and {2} expected zones for groups should result in {3} instance count by availability zone")
    @MethodSource("testAvailabilityZoneDistributionForWholeInstanceGroupData")
    void testPopulateShouldDistributeNodesAcrossInstancesOfTheGatewayAndAuxiliaryGroups(int gatewayInstanceCount, int auxiliaryInstanceCount,
            Set<String> expectedZonesForGroups, Map<String, Integer> expectedInstanceCountByAz) {
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setMultiAz(Boolean.TRUE);
        stack.setType(StackType.DATALAKE);
        stack.getInstanceGroups()
                .forEach(ig -> {
                    ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(expectedZonesForGroups.size(), ig.getId(), ig));
                    ig.setAvailabilityZones(getAvailabilityZoneSet(expectedZonesForGroups, ig));
                });
        if (gatewayInstanceCount > 0) {
            stack.getInstanceGroups().add(getInstanceGroup(expectedZonesForGroups, 4L, InstanceGroupName.GATEWAY, stack, gatewayInstanceCount));
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
                    .filter(ig -> InstanceGroupName.GATEWAY.getName().equals(ig.getGroupName())
                            || InstanceGroupName.AUXILIARY.getName().equals(ig.getGroupName()))
                    .flatMap(ig -> ig.getNotTerminatedInstanceMetaDataSet().stream())
                    .filter(im -> expectedZone.equals(im.getAvailabilityZone()))
                    .count();
            assertEquals(Long.valueOf(expectedCountByZone), actualCount);
        }
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
}