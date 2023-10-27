package com.sequenceiq.cloudbreak.service.multiaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
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

    @Captor
    private ArgumentCaptor<Set<InstanceMetaData>> savedInstanceMetadatas;

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
        List<String> groupAvailabilityZonesForMetadata = List.of("1", "2");
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.getInstanceGroups()
                .forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));


        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        when(multiAzCalculatorService.determineRackId(any(), eq("3"))).thenReturn("/3");
        underTest.populate(1L);

        Set<InstanceMetaData> instancesExpectedToBeUpdated = new HashSet<>(stack.getNotDeletedInstanceMetaDataSet());
        verify(instanceMetaDataService).saveAll(savedInstanceMetadatas.capture());
        verify(instanceMetaDataService, times(1)).saveAll(any());
        assertTrue(savedInstanceMetadatas.getValue().size() == 3);
        assertTrue(savedInstanceMetadatas.getValue().stream()
                .allMatch(im -> groupAvailabilityZonesForGroup.contains(im.getAvailabilityZone())));
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

        boolean actual = underTest.populateForScaling(stack, Set.of(), Boolean.FALSE, NetworkScaleDetails.getEmpty());

        assertFalse(actual);
        verifyNoInteractions(instanceMetaDataService);

    }

    @Test
    void testPopulateForScalingWhenStackIsNotMultiAzEnabledWithTargetedAzs() {
        List<String> groupAvailabilityZonesForMetadata = List.of("1", "2");
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.setMultiAz(Boolean.FALSE);
        Set<String> targetedAzs = Set.of("1", "2");
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        boolean actual = underTest.populateForScaling(stack, Set.of(), Boolean.FALSE, new NetworkScaleDetails(List.of(), targetedAzs));

        assertFalse(actual);
        verifyNoInteractions(instanceMetaDataService);
        notDeletedInstanceMetaDataSet.stream()
                .filter(e -> e.getInstanceStatus().equals(InstanceStatus.CREATED))
                .forEach(e -> assertTrue(Strings.isNullOrEmpty(e.getAvailabilityZone())));
    }

    @Test
    void testPopulateForScalingWhenStackPlatformIsNotSupportedFromConnectorSide() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.TRUE);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(null);

        boolean actual = underTest.populateForScaling(stack, Set.of(), Boolean.FALSE, NetworkScaleDetails.getEmpty());

        assertFalse(actual);
        verifyNoInteractions(instanceMetaDataService);
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededButInstanceGroupNamesSetIsEmpty() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.TRUE);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);

        boolean actual = underTest.populateForScaling(stack, Set.of(), Boolean.FALSE, NetworkScaleDetails.getEmpty());

        assertFalse(actual);
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataByStackId(stack.getId());
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndUpscale() {
        List<String> groupAvailabilityZonesForMetadata = List.of("1", "2");
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.getInstanceGroups()
                .forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));

        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);

        boolean actual = underTest.populateForScaling(stack, groupNamesToScale, Boolean.FALSE, NetworkScaleDetails.getEmpty());

        assertTrue(actual);
        verify(instanceMetaDataService).saveAll(savedInstanceMetadatas.capture());
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataByStackId(stack.getId());
        verify(instanceMetaDataService, times(0)).getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString());
        assertTrue(savedInstanceMetadatas.getValue().size() == 3);
        savedInstanceMetadatas.getValue()
                .forEach(im -> assertTrue(StringUtils.isNotEmpty(im.getAvailabilityZone())));
        savedInstanceMetadatas.getValue()
                .forEach(im -> assertTrue(groupAvailabilityZonesForGroup.contains(im.getAvailabilityZone())));
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndUpscaleWhenTargetedAzs() {
        Set<String> targetedAzs = Set.of("1", "2");
        List<String> groupAvailabilityZonesForMetadata = List.of("1", "2");
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.getInstanceGroups()
                .forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));

        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);

        boolean actual = underTest.populateForScaling(stack, groupNamesToScale, Boolean.FALSE, new NetworkScaleDetails(List.of(), targetedAzs));

        assertTrue(actual);
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataByStackId(stack.getId());
        verify(instanceMetaDataService, times(1)).saveAll(any());
        verify(instanceMetaDataService, times(0)).getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString());
        verify(instanceMetaDataService).saveAll(savedInstanceMetadatas.capture());
        assertTrue(savedInstanceMetadatas.getValue().size() == 3);
        savedInstanceMetadatas.getValue()
                .forEach(im -> assertTrue(StringUtils.isNotEmpty(im.getAvailabilityZone())));
        savedInstanceMetadatas.getValue()
                .forEach(im -> assertTrue(groupAvailabilityZonesForGroup.contains(im.getAvailabilityZone())));
        savedInstanceMetadatas.getValue()
                .stream()
                .filter(e -> e.getInstanceStatus().equals(InstanceStatus.CREATED))
                .forEach(e -> assertTrue(targetedAzs.contains(e.getAvailabilityZone())));
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndRepair() {
        boolean repair = Boolean.TRUE;
        List<String> groupAvailabilityZonesForMetadata = List.of();
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        Map<String, String> expectedAvailabilityZoneByFqdn = new HashMap<>();
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.getInstanceGroups()
                .forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));

        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);

        int index = 0;
        List<String> availabilityZoneList = new ArrayList<>(groupAvailabilityZonesForGroup);
        for (InstanceMetaData im : stack.getNotDeletedInstanceMetaDataSet()) {
            im.setInstanceStatus(InstanceStatus.REQUESTED);
            String discoveryFQDN = im.getDiscoveryFQDN();
            String expectedZoneForInstance = availabilityZoneList.get(index % groupAvailabilityZonesForGroup.size());
            expectedAvailabilityZoneByFqdn.put(discoveryFQDN, expectedZoneForInstance);
            when(instanceMetaDataService.getAvailabilityZoneFromDiskIfRepair(stack, repair, im.getInstanceGroup().getGroupName(), discoveryFQDN))
                    .thenReturn(expectedZoneForInstance);
            when(multiAzCalculatorService.determineRackId(im.getSubnetId(), expectedZoneForInstance)).thenReturn("/" + expectedZoneForInstance);
            index++;
        }

        boolean actual = underTest.populateForScaling(stack, groupNamesToScale, repair, NetworkScaleDetails.getEmpty());

        assertTrue(actual);
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataByStackId(stack.getId());
        verify(instanceMetaDataService, times(stack.getNotDeletedInstanceMetaDataSet().size()))
                .getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString());
        verify(instanceMetaDataService).saveAll(savedInstanceMetadatas.capture());
        assertTrue(savedInstanceMetadatas.getValue().size() == 3);
        savedInstanceMetadatas.getValue().forEach(instanceMetaData -> {
            String discoveryFQDN = instanceMetaData.getDiscoveryFQDN();
            String expectedAz = expectedAvailabilityZoneByFqdn.get(discoveryFQDN);
            assertEquals(expectedAz, instanceMetaData.getAvailabilityZone());
            assertEquals("/" + expectedAz, instanceMetaData.getRackId());
        });
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndRepairButAzCouldNotBeFoundInVolume() {
        boolean repair = Boolean.TRUE;
        List<String> groupAvailabilityZonesForMetadata = List.of("1", "2");
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.getInstanceGroups()
                .forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));

        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);
        notDeletedInstanceMetaDataSet.forEach(im -> im.setInstanceStatus(InstanceStatus.REQUESTED));
        when(instanceMetaDataService.getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString())).thenReturn(null);

        assertThrows(CloudbreakServiceException.class, () -> underTest.populateForScaling(stack, groupNamesToScale, repair, NetworkScaleDetails.getEmpty()));
    }

    private Stack getStackWithGroupsAndInstances(List<String> groupAvailabilityZonesForMetadata, List<String> groupAvailabilityZonesForGroup) {
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setMultiAz(Boolean.TRUE);

        for (InstanceGroup ig : stack.getInstanceGroups()) {
            ig.setInstanceMetaData(getInstanceMetaData(groupAvailabilityZonesForMetadata.size(), groupAvailabilityZonesForMetadata, ig, Set.of()));
            ig.setAvailabilityZones(getAvailabilityZones(groupAvailabilityZonesForGroup, ig));
        }

        return stack;
    }

    private Set<InstanceMetaData> getInstanceMetaData(int numberOfMetadata, List<String> groupAvailabilityZones, InstanceGroup ig, Set<String> nullableAzs) {
        int numberOfInstances = numberOfMetadata;
        Set<InstanceMetaData> instanceMetaDatas = TestUtil.generateInstanceMetaDatas(numberOfInstances, ig.getId(), ig);
        setupInstanceMetadataZones(groupAvailabilityZones, instanceMetaDatas);
        deleteAzInformation(nullableAzs, instanceMetaDatas);
        return instanceMetaDatas;
    }

    private void deleteAzInformation(Set<String> nullableAzs, Set<InstanceMetaData> instanceMetaDatas) {
        int i = 1;
        for (InstanceMetaData instanceMetaData : instanceMetaDatas) {
            if (!Strings.isNullOrEmpty(instanceMetaData.getAvailabilityZone()) && nullableAzs.contains(instanceMetaData.getAvailabilityZone())) {
                instanceMetaData.setAvailabilityZone(null);
                instanceMetaData.setRackId(null);
                instanceMetaData.setInstanceStatus(InstanceStatus.REQUESTED);
            }
        }
    }

    private void setupInstanceMetadataZones(List<String> groupAvailabilityZones, Set<InstanceMetaData> instanceMetaDatas) {
        int index = 0;
        for (InstanceMetaData instanceMetaData : instanceMetaDatas) {
            if (!groupAvailabilityZones.isEmpty()) {
                String az = groupAvailabilityZones.get(index % groupAvailabilityZones.size());
                instanceMetaData.setAvailabilityZone(az);
                instanceMetaData.setRackId("/" + az);
            }
            index++;
        }
    }

    private Set<AvailabilityZone> getAvailabilityZones(List<String> groupAvailabilityZones, InstanceGroup ig) {
        Set<AvailabilityZone> availabilityZones = groupAvailabilityZones.stream().map(az -> {
            AvailabilityZone availabilityZone = new AvailabilityZone();
            availabilityZone.setAvailabilityZone(az);
            availabilityZone.setInstanceGroup(ig);
            return availabilityZone;
        }).collect(Collectors.toSet());
        return availabilityZones;
    }
}
