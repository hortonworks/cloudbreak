package com.sequenceiq.cloudbreak.service.multiaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@ExtendWith(MockitoExtension.class)
class InstanceMetadataAvailabilityZoneCalculatorTest {

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private MultiAzCalculatorService multiAzCalculatorService;

    @Mock
    private AvailabilityZoneConnector availabilityZoneConnector;

    @InjectMocks
    private InstanceMetadataAvailabilityZoneCalculator underTest;

    @BeforeAll
    static void beforeAll() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);
    }

    @Test
    void testPopulateWhenTheStackCouldNotBeFoundShouldThrowNotFoundException() {
        when(stackService.getByIdWithLists(anyLong())).thenThrow(new NotFoundException("Stack not found with id"));

        assertThrows(NotFoundException.class, () -> underTest.populate(1L));

        verifyNoInteractions(instanceMetaDataService);
    }

    @Test
    void testPopulateWhenTheStackIsNotMultiAzEnabledShouldDoNothing() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.FALSE);
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);

        underTest.populate(1L);

        verifyNoInteractions(instanceMetaDataService);
    }

    @Test
    void testPopulateWhenTheStackIsMultiAzEnabledButAvailabilityZoneConnectorDoesNotExistForPlatformShouldDoNothing() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.TRUE);
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(null);

        underTest.populate(1L);

        verifyNoInteractions(instanceMetaDataService);
    }

    @Test
    void testPopulateWhenTheStackIsMultiAzEnabledAndNoAzConfiguredOnTheInstanceGroupLevelShouldThrowException() {
        Set<String> environmentAvailabilityZones = Set.of("1", "2", "3");
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setMultiAz(Boolean.TRUE);
        stack.getInstanceGroups()
                .forEach(ig -> ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(environmentAvailabilityZones.size(), ig.getId(), ig)));
        stack.getNotTerminatedInstanceMetaDataSet().forEach(instance -> instance.setAvailabilityZone("1"));
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);

        Assertions.assertThrows(CloudbreakServiceException.class,
                () -> underTest.populate(1L));

        verifyNoInteractions(instanceMetaDataService);
    }

    @Test
    void testPopulateWhenTheStackIsMultiAzEnabledButSomeOfTheInstancesHaveAzConfigAlready() {
        Set<String> groupAvailabilityZones = Set.of("1", "2", "3");
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZones);
        Set<InstanceMetaData> instancesWithAzConfig = stack.getInstanceGroupsAsList().get(0).getInstanceMetaData();
        instancesWithAzConfig.forEach(im -> im.setAvailabilityZone("1"));
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        when(multiAzCalculatorService.determineRackId(any(), eq("1"))).thenReturn("/1");
        when(multiAzCalculatorService.determineRackId(any(), eq("2"))).thenReturn("/2");
        when(multiAzCalculatorService.determineRackId(any(), eq("3"))).thenReturn("/3");

        underTest.populate(1L);

        Set<InstanceMetaData> instancesExpectedToBeUpdated = new HashSet<>(stack.getNotDeletedInstanceMetaDataSet());
        instancesExpectedToBeUpdated.removeAll(instancesWithAzConfig);
        verify(instanceMetaDataService).saveAll(instancesExpectedToBeUpdated);
        assertTrue(stack.getInstanceGroups().stream()
                .allMatch(ig -> ig.getInstanceMetaData().stream()
                        .allMatch(im -> groupAvailabilityZones.contains(im.getAvailabilityZone()))));
        assertTrue(instancesExpectedToBeUpdated.stream()
                .allMatch(im -> ("/" + im.getAvailabilityZone()).equals(im.getRackId())));
    }

    @Test
    void testPopulateWhenTheStackIsMultiAzEnabledAndAzConfiguredOnGroupNetworkAndTheInstanceLevelAlreadyShouldNotTouchAzSettings() {
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        Set<String> groupAvailabilityZones = Set.of("2");
        stack.setMultiAz(Boolean.TRUE);
        stack.getInstanceGroups()
                .forEach(ig -> {
                    ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(3, ig.getId(), ig));
                    Set<AvailabilityZone> availabilityZones = groupAvailabilityZones.stream().map(az -> {
                        AvailabilityZone availabilityZone = new AvailabilityZone();
                        availabilityZone.setAvailabilityZone(az);
                        availabilityZone.setInstanceGroup(ig);
                        return availabilityZone;
                    }).collect(Collectors.toSet());
                    ig.setAvailabilityZones(availabilityZones);
                });
        stack.getNotTerminatedInstanceMetaDataSet().forEach(instance -> instance.setAvailabilityZone("1"));
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);

        underTest.populate(1L);

        verifyNoInteractions(instanceMetaDataService);
        stack.getNotTerminatedInstanceMetaDataSet().forEach(instance -> assertEquals("1", instance.getAvailabilityZone()));
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] testAvailabilityZoneDistributionForWholeInstanceGroupData() {
        return new Object[][]{
                //instanceCountByGroup, groupLevelZones,        expectedInstanceCountByAz
                {17,                   Set.of("1"),            Map.of("1", 17, "2", 0, "3", 0)},
                {17,                   Set.of("2", "3"),       Map.of("1", 0, "2", 9, "3", 8)},
                {19,                   Set.of("1", "2", "3"),  Map.of("1", 7, "2", 6, "3", 6)},
                {37,                   Set.of("1", "2", "3"),  Map.of("1", 13, "2", 12, "3", 12)},
                {41,                   Set.of("1", "2", "3"),  Map.of("1", 14, "2", 14, "3", 13)},
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "testPopulateShouldDistributeNodesAcrossInstancesOfTheGroup settings " +
            "when {0} environment level zones and {1} instances count and {2} group level zones should result in {3} subnet counts")
    @MethodSource("testAvailabilityZoneDistributionForWholeInstanceGroupData")
    void testPopulateShouldDistributeNodesAcrossInstancesOfTheGroup(int instanceCountByGroup, Set<String> groupAvailabilityZones,
            Map<String, Integer> expectedInstanceCountByAz) {
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setMultiAz(Boolean.TRUE);
        stack.getInstanceGroups()
                .forEach(ig -> ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(instanceCountByGroup, ig.getId(), ig)));
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        if (CollectionUtils.isNotEmpty(groupAvailabilityZones)) {
            stack.getInstanceGroups()
                    .forEach(ig -> {
                        Set<AvailabilityZone> availabilityZones = groupAvailabilityZones.stream().map(az -> {
                            AvailabilityZone availabilityZone = new AvailabilityZone();
                            availabilityZone.setAvailabilityZone(az);
                            availabilityZone.setInstanceGroup(ig);
                            return availabilityZone;
                        }).collect(Collectors.toSet());
                        ig.setAvailabilityZones(availabilityZones);
                    });
        }

        underTest.populate(1L);

        verify(instanceMetaDataService).saveAll(stack.getNotDeletedInstanceMetaDataSet());
        for (Map.Entry<String, Integer> expectedCountByAzEntry : expectedInstanceCountByAz.entrySet()) {
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                long actualInstanceCountByAz = instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                        .filter(im -> expectedCountByAzEntry.getKey().equals(im.getAvailabilityZone()))
                        .count();
                assertEquals(Long.valueOf(expectedCountByAzEntry.getValue()), actualInstanceCountByAz);
            }
        }
    }

    @Test
    void testPopulateForScalingWhenStackIsNotMultiAzEnabled() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.FALSE);

        boolean actual = underTest.populateForScaling(stack, Set.of(), Boolean.FALSE);

        Assertions.assertFalse(actual);
        verifyNoInteractions(instanceMetaDataService);

    }

    @Test
    void testPopulateForScalingWhenStackPlatformIsNotSupportedFromConnectorSide() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.TRUE);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(null);

        boolean actual = underTest.populateForScaling(stack, Set.of(), Boolean.FALSE);

        Assertions.assertFalse(actual);
        verifyNoInteractions(instanceMetaDataService);
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededButInstanceGroupNamesSetIsEmpty() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.TRUE);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);

        boolean actual = underTest.populateForScaling(stack, Set.of(), Boolean.FALSE);

        Assertions.assertFalse(actual);
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataByStackId(stack.getId());
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndUpscale() {
        Set<String> groupAvailabilityZones = Set.of("1", "2", "3");
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZones);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);

        boolean actual = underTest.populateForScaling(stack, groupNamesToScale, Boolean.FALSE);

        Assertions.assertTrue(actual);
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataByStackId(stack.getId());
        verify(instanceMetaDataService, times(1)).saveAll(notDeletedInstanceMetaDataSet);
        verify(instanceMetaDataService, times(0)).getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString());
        notDeletedInstanceMetaDataSet
                .forEach(im -> assertTrue(StringUtils.isNotEmpty(im.getAvailabilityZone())));
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndRepair() {
        boolean repair = Boolean.TRUE;
        Set<String> groupAvailabilityZones = Set.of("1", "2", "3");
        Map<String, String> expectedAvailabilityZoneByFqdn = new HashMap<>();
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZones);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);

        int index = 0;
        List<String> availabilityZoneList = new ArrayList<>(groupAvailabilityZones);
        for (InstanceMetaData im : notDeletedInstanceMetaDataSet) {
            im.setInstanceStatus(InstanceStatus.REQUESTED);
            String discoveryFQDN = im.getDiscoveryFQDN();
            String expectedZoneForInstance = availabilityZoneList.get(index % groupAvailabilityZones.size());
            expectedAvailabilityZoneByFqdn.put(discoveryFQDN, expectedZoneForInstance);
            when(instanceMetaDataService.getAvailabilityZoneFromDiskIfRepair(stack, repair, im.getInstanceGroup().getGroupName(), discoveryFQDN))
                    .thenReturn(expectedZoneForInstance);
            when(multiAzCalculatorService.determineRackId(im.getSubnetId(), expectedZoneForInstance)).thenReturn("/" + expectedZoneForInstance);
            index++;
        }

        boolean actual = underTest.populateForScaling(stack, groupNamesToScale, repair);

        Assertions.assertTrue(actual);
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataByStackId(stack.getId());
        verify(instanceMetaDataService, times(1)).saveAll(notDeletedInstanceMetaDataSet);
        verify(instanceMetaDataService, times(notDeletedInstanceMetaDataSet.size()))
                .getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString());
        notDeletedInstanceMetaDataSet.forEach(instanceMetaData -> {
            String discoveryFQDN = instanceMetaData.getDiscoveryFQDN();
            String expectedAz = expectedAvailabilityZoneByFqdn.get(discoveryFQDN);
            assertEquals(expectedAz, instanceMetaData.getAvailabilityZone());
            assertEquals("/" + expectedAz, instanceMetaData.getRackId());
            verify(instanceMetaDataService).getAvailabilityZoneFromDiskIfRepair(stack, repair, instanceMetaData.getInstanceGroupName(), discoveryFQDN);
        });
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndRepairButAzCouldNotBeFoundInVolume() {
        boolean repair = Boolean.TRUE;
        Set<String> groupAvailabilityZones = Set.of("1", "2", "3");
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZones);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);
        notDeletedInstanceMetaDataSet.forEach(im -> im.setInstanceStatus(InstanceStatus.REQUESTED));
        when(instanceMetaDataService.getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString())).thenReturn(null);

        assertThrows(CloudbreakServiceException.class, () -> underTest.populateForScaling(stack, groupNamesToScale, repair));
    }

    private static Stack getStackWithGroupsAndInstances(Set<String> groupAvailabilityZones) {
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setMultiAz(Boolean.TRUE);
        stack.getInstanceGroups()
                .forEach(ig -> {
                    ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(groupAvailabilityZones.size(), ig.getId(), ig));
                    Set<AvailabilityZone> availabilityZones = groupAvailabilityZones.stream().map(az -> {
                        AvailabilityZone availabilityZone = new AvailabilityZone();
                        availabilityZone.setAvailabilityZone(az);
                        availabilityZone.setInstanceGroup(ig);
                        return availabilityZone;
                    }).collect(Collectors.toSet());
                    ig.setAvailabilityZones(availabilityZones);
                });
        return stack;
    }
}
