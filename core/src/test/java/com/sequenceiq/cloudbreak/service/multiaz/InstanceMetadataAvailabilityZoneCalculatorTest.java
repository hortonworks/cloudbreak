package com.sequenceiq.cloudbreak.service.multiaz;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.service.cluster.InstanceGroupSubnetCollector;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@ExtendWith(MockitoExtension.class)
class InstanceMetadataAvailabilityZoneCalculatorTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    @Mock
    private StackService stackService;

    @Mock
    private InstanceGroupSubnetCollector instanceGroupSubnetCollector;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private AvailabilityZoneConnector availabilityZoneConnector;

    @Mock
    private DetailedEnvironmentResponse environmentResponse;

    @Mock
    private EnvironmentNetworkResponse environmentNetworkResponse;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private EnvironmentService environmentClientService;

    @Captor
    private ArgumentCaptor<Set<InstanceMetaData>> savedInstanceMetadatas;

    @InjectMocks
    private InstanceMetadataAvailabilityZoneCalculator underTest;

    @BeforeAll
    static void beforeAll() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);
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

    static Object[][] testAvailabilityZoneDistributionForAwsInstanceGroupData() {
        return new Object[][]{
                //instanceCountByGroup, groupLevelZones,        expectedInstanceCountByAz,  expectedInstanceCountBySubnet
                {17,                   Set.of("eu-central-1a"),            Map.of("eu-central-1a", 17, "eu-central-1b", 0, "eu-central-1c", 0), Map.of("subnet1", 17, "subnet2", 0, "subnet3", 0)},
                {17,                   Set.of("eu-central-1b", "eu-central-1c"),       Map.of("eu-central-1a", 0, "eu-central-1b", 8, "eu-central-1c", 9), Map.of("subnet1", 0, "subnet2", 8, "subnet3", 9)},
                {19,                   Set.of("eu-central-1a", "eu-central-1b", "eu-central-1c"),  Map.of("eu-central-1a", 7, "eu-central-1b", 6, "eu-central-1c", 6), Map.of("subnet1", 7, "subnet2", 6, "subnet3", 6)},
                {37,                   Set.of("eu-central-1a", "eu-central-1b", "eu-central-1c"),  Map.of("eu-central-1a", 13, "eu-central-1b", 12, "eu-central-1c", 12), Map.of("subnet1", 13, "subnet2", 12, "subnet3", 12)},
                {41,                   Set.of("eu-central-1a", "eu-central-1b", "eu-central-1c"),  Map.of("eu-central-1a", 14, "eu-central-1b", 13, "eu-central-1c", 14), Map.of("subnet1", 14, "subnet2", 13, "subnet3", 14)},
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

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
        String subnetId = "aSubnetId";
        List<String> groupAvailabilityZonesForMetadata = List.of("1", "2");
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.setNetwork(TestUtil.networkWithSubnetId(subnetId));
        stack.getInstanceGroups()
                .forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));


        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        underTest.populate(1L);

        Set<InstanceMetaData> instancesExpectedToBeUpdated = new HashSet<>(stack.getNotDeletedInstanceMetaDataSet());
        verify(instanceMetaDataService).saveAll(savedInstanceMetadatas.capture());
        verify(instanceMetaDataService, times(1)).saveAll(any());
        assertTrue(savedInstanceMetadatas.getValue().size() == 3);
        assertTrue(savedInstanceMetadatas.getValue().stream()
                .allMatch(im -> groupAvailabilityZonesForGroup.contains(im.getAvailabilityZone())));
        assertTrue(instancesExpectedToBeUpdated.stream()
                .allMatch(im -> ("/" + im.getAvailabilityZone()).equals(im.getRackId())));
        Assertions.assertTrue(savedInstanceMetadatas.getValue().stream()
                .allMatch(im -> isNotEmpty(im.getSubnetId()) && subnetId.equals(im.getSubnetId())));
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

    @ParameterizedTest(name = "testPopulateShouldDistributeNodesAcrossInstancesOfTheGroup settings " +
            "when {0} environment level zones and {1} instances count and {2} group level zones should result in {3} subnet counts")
    @MethodSource("testAvailabilityZoneDistributionForWholeInstanceGroupData")
    void testPopulateShouldDistributeNodesAcrossInstancesOfTheGroup(int instanceCountByGroup, Set<String> groupAvailabilityZones,
            Map<String, Integer> expectedInstanceCountByAz) {
        String subnetId = "aSubnetId";
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.azureCredential());
        stack.setNetwork(TestUtil.networkWithSubnetId(subnetId));
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
        Assertions.assertTrue(stack.getNotDeletedInstanceMetaDataSet().stream()
                .allMatch(im -> isNotEmpty(im.getSubnetId()) && subnetId.equals(im.getSubnetId())));
    }

    @ParameterizedTest(name = "testPopulateShouldDistributeAwsNodesAcrossInstancesOfTheGroup settings " +
            "when {0} environment level zones and {1} instances count and {2} group level zones should result in {3} subnet counts")
    @MethodSource("testAvailabilityZoneDistributionForAwsInstanceGroupData")
    void testPopulateShouldDistributeAwsNodesAcrossInstancesOfTheGroup(int instanceCountByGroup, Set<String> groupAvailabilityZones,
            Map<String, Integer> expectedInstanceCountByAz, Map<String, Integer> expectedInstanceCountBySubnet) {
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Stack stack = TestUtil.stack(Status.REQUESTED, TestUtil.awsCredential());
        stack.setMultiAz(Boolean.TRUE);
        stack.getInstanceGroups()
                .forEach(ig -> {
                    ig.setInstanceMetaData(TestUtil.generateInstanceMetaDatas(instanceCountByGroup, ig.getId(), ig));
                    ig.setStack(stack);
                });
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform(CloudPlatform.AWS.name());
        instanceGroupNetwork.setAttributes(new Json("{\"subnetIds\":[\"subnet1\",\"subnet2\",\"subnet3\"]}"));
        stack.getInstanceGroups().forEach(ig -> ig.setInstanceGroupNetwork(instanceGroupNetwork));
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environmentResponse);
        when(environmentResponse.getNetwork()).thenReturn(environmentNetworkResponse);
        when(environmentNetworkResponse.getSubnetMetas())
                .thenReturn(Map.of("subnet1", new CloudSubnet.Builder().id("id1").name("name1").availabilityZone("eu-central-1a").cidr("cidr1").build(),
                        "subnet2", new CloudSubnet.Builder().id("id2").name("name2").availabilityZone("eu-central-1b").cidr("cidr2").build(),
                        "subnet3", new CloudSubnet.Builder().id("id3").name("name3").availabilityZone("eu-central-1c").cidr("cidr3").build()));
        when(instanceGroupSubnetCollector.collect(any(), any())).thenReturn(Set.of("subnet1", "subnet2", "subnet3"));
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

        for (Map.Entry<String, Integer> expectedCountBySubnetEntry : expectedInstanceCountBySubnet.entrySet()) {
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                long actualInstanceCountByAz = instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                        .filter(im -> expectedCountBySubnetEntry.getKey().equals(im.getSubnetId()))
                        .count();
                assertEquals(Long.valueOf(expectedCountBySubnetEntry.getValue()), actualInstanceCountByAz);
            }
        }
        Assertions.assertTrue(stack.getNotDeletedInstanceMetaDataSet().stream().allMatch(im -> isNotEmpty(im.getSubnetId())));
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
        Assertions.assertTrue(stack.getNotDeletedInstanceMetaDataSet().stream()
                .filter(e -> e.getInstanceStatus().equals(InstanceStatus.CREATED))
                .allMatch(im -> isNotEmpty(im.getSubnetId())));
    }

    @Test
    void testPopulateForScalingWhenStackPlatformIsNotSupportedFromConnectorSide() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.TRUE);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(null);

        boolean actual = underTest.populateForScaling(stack, Set.of(), Boolean.FALSE, NetworkScaleDetails.getEmpty());

        assertFalse(actual);
        verifyNoInteractions(instanceMetaDataService);
        Assertions.assertTrue(stack.getNotDeletedInstanceMetaDataSet().stream()
                .filter(e -> e.getInstanceStatus().equals(InstanceStatus.CREATED))
                .allMatch(im -> isNotEmpty(im.getSubnetId())));
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededButInstanceGroupNamesSetIsEmpty() {
        Stack stack = TestUtil.stack();
        stack.setMultiAz(Boolean.TRUE);
        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);

        boolean actual = underTest.populateForScaling(stack, Set.of(), Boolean.FALSE, NetworkScaleDetails.getEmpty());

        assertFalse(actual);
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId());
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndUpscale() {
        List<String> groupAvailabilityZonesForMetadata = List.of("1", "2");
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        String subnetId = "aSubnetId";
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.setNetwork(TestUtil.networkWithSubnetId(subnetId));
        stack.getInstanceGroups()
                .forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));

        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);

        boolean actual = underTest.populateForScaling(stack, groupNamesToScale, Boolean.FALSE, NetworkScaleDetails.getEmpty());

        assertTrue(actual);
        verify(instanceMetaDataService).saveAll(savedInstanceMetadatas.capture());
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId());
        verify(instanceMetaDataService, times(0)).getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString());
        assertTrue(savedInstanceMetadatas.getValue().size() == 3);
        savedInstanceMetadatas.getValue()
                .forEach(im -> assertTrue(isNotEmpty(im.getAvailabilityZone())));
        savedInstanceMetadatas.getValue()
                .forEach(im -> assertTrue(groupAvailabilityZonesForGroup.contains(im.getAvailabilityZone())));
        Assertions.assertTrue(savedInstanceMetadatas.getValue().stream().allMatch(im -> subnetId.equals(im.getSubnetId())));
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndUpscaleWhenTargetedAzs() {
        Set<String> targetedAzs = Set.of("1", "2");
        List<String> groupAvailabilityZonesForMetadata = List.of("1", "2");
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        String subnetId = "aSubnetId";
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.setNetwork(TestUtil.networkWithSubnetId(subnetId));
        stack.getInstanceGroups()
                .forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));

        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);

        boolean actual = underTest.populateForScaling(stack, groupNamesToScale, Boolean.FALSE, new NetworkScaleDetails(List.of(), targetedAzs));

        assertTrue(actual);
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId());
        verify(instanceMetaDataService, times(1)).saveAll(any());
        verify(instanceMetaDataService, times(0)).getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString());
        verify(instanceMetaDataService).saveAll(savedInstanceMetadatas.capture());
        assertTrue(savedInstanceMetadatas.getValue().size() == 3);
        savedInstanceMetadatas.getValue()
                .forEach(im -> assertTrue(isNotEmpty(im.getAvailabilityZone())));
        savedInstanceMetadatas.getValue()
                .forEach(im -> assertTrue(groupAvailabilityZonesForGroup.contains(im.getAvailabilityZone())));
        savedInstanceMetadatas.getValue()
                .stream()
                .filter(e -> e.getInstanceStatus().equals(InstanceStatus.CREATED))
                .forEach(e -> assertTrue(targetedAzs.contains(e.getAvailabilityZone())));
        Assertions.assertTrue(savedInstanceMetadatas.getValue().stream().allMatch(im -> subnetId.equals(im.getSubnetId())));
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndRepair() {
        boolean repair = Boolean.TRUE;
        List<String> groupAvailabilityZonesForMetadata = List.of();
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        Map<String, String> expectedAvailabilityZoneByFqdn = new HashMap<>();
        String subnetId = "aSubnetId";
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.setNetwork(TestUtil.networkWithSubnetId(subnetId));
        stack.getInstanceGroups()
                .forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));

        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);

        int index = 0;
        List<String> availabilityZoneList = new ArrayList<>(groupAvailabilityZonesForGroup);
        for (InstanceMetaData im : stack.getNotDeletedInstanceMetaDataSet()) {
            im.setInstanceStatus(InstanceStatus.REQUESTED);
            String discoveryFQDN = im.getDiscoveryFQDN();
            String expectedZoneForInstance = availabilityZoneList.get(index % groupAvailabilityZonesForGroup.size());
            expectedAvailabilityZoneByFqdn.put(discoveryFQDN, expectedZoneForInstance);
            when(instanceMetaDataService.getAvailabilityZoneFromDiskIfRepair(stack, repair, im.getInstanceGroup().getGroupName(), discoveryFQDN))
                    .thenReturn(expectedZoneForInstance);
            index++;
        }

        boolean actual = underTest.populateForScaling(stack, groupNamesToScale, repair, NetworkScaleDetails.getEmpty());

        assertTrue(actual);
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId());
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
        Assertions.assertTrue(savedInstanceMetadatas.getValue().stream().allMatch(im -> subnetId.equals(im.getSubnetId())));
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndRepairForAws() {
        boolean repair = Boolean.TRUE;
        Map<String, String> expectedSubnetIdForAz = Map.of("az1", "subnet1", "az2", "subnet2", "az3", "subnet3");
        List<String> groupAvailabilityZonesForMetadata = List.of();
        List<String> groupAvailabilityZonesForGroup = List.of("az1", "az2", "az3");
        when(instanceGroupSubnetCollector.collect(any(), any())).thenReturn(expectedSubnetIdForAz.values().stream().collect(Collectors.toSet()));
        Map<String, String> expectedAvailabilityZoneByFqdn = new HashMap<>();
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.getInstanceGroups().forEach(ig -> ig.setStack(stack));
        stack.getInstanceGroups().forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform(CloudPlatform.AWS.name());
        instanceGroupNetwork.setAttributes(new Json("{\"subnetIds\":[\"subnet1\",\"subnet2\",\"subnet3\"]}"));
        stack.getInstanceGroups().forEach(ig -> ig.setInstanceGroupNetwork(instanceGroupNetwork));
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environmentResponse);
        when(environmentResponse.getNetwork()).thenReturn(environmentNetworkResponse);
        when(environmentNetworkResponse.getSubnetMetas())
                .thenReturn(Map.of("subnet1", new CloudSubnet.Builder().id("id1").name("name1").availabilityZone("az1").cidr("cidr1").build(),
                        "subnet2", new CloudSubnet.Builder().id("id2").name("name2").availabilityZone("az2").cidr("cidr2").build(),
                        "subnet3", new CloudSubnet.Builder().id("id3").name("name3").availabilityZone("az3").cidr("cidr3").build()));

        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);

        int index = 0;
        List<String> availabilityZoneList = new ArrayList<>(groupAvailabilityZonesForGroup);
        for (InstanceMetaData im : stack.getNotDeletedInstanceMetaDataSet()) {
            im.setInstanceStatus(InstanceStatus.REQUESTED);
            String discoveryFQDN = im.getDiscoveryFQDN();
            String expectedZoneForInstance = availabilityZoneList.get(index % groupAvailabilityZonesForGroup.size());
            expectedAvailabilityZoneByFqdn.put(discoveryFQDN, expectedZoneForInstance);
            when(instanceMetaDataService.getAvailabilityZoneFromDiskIfRepair(stack, repair, im.getInstanceGroup().getGroupName(), discoveryFQDN))
                    .thenReturn(expectedZoneForInstance);
            index++;
        }

        boolean actual = underTest.populateForScaling(stack, groupNamesToScale, repair, NetworkScaleDetails.getEmpty());

        assertTrue(actual);
        verify(instanceMetaDataService, times(1)).getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId());
        verify(instanceMetaDataService, times(stack.getNotDeletedInstanceMetaDataSet().size()))
                .getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString());
        verify(instanceMetaDataService).saveAll(savedInstanceMetadatas.capture());
        assertTrue(savedInstanceMetadatas.getValue().size() == 3);
        savedInstanceMetadatas.getValue().forEach(instanceMetaData -> {
            String discoveryFQDN = instanceMetaData.getDiscoveryFQDN();
            String expectedAz = expectedAvailabilityZoneByFqdn.get(discoveryFQDN);
            String expectedSubnetId = expectedSubnetIdForAz.get(expectedAz);
            assertEquals(expectedAz, instanceMetaData.getAvailabilityZone());
            assertEquals(expectedSubnetId, instanceMetaData.getSubnetId());
            assertEquals("/" + expectedAz, instanceMetaData.getRackId());
        });
        Assertions.assertTrue(savedInstanceMetadatas.getValue().stream()
                .allMatch(im -> expectedSubnetIdForAz.values().contains(im.getSubnetId())));
    }

    @Test
    void testPopulateForScalingWhenPopulationIsNeededAndRepairButAzCouldNotBeFoundInVolume() {
        boolean repair = Boolean.TRUE;
        List<String> groupAvailabilityZonesForMetadata = List.of("1", "2");
        List<String> groupAvailabilityZonesForGroup = List.of("1", "2", "3");
        String subnetId = "aSubnetId";
        Stack stack = getStackWithGroupsAndInstances(groupAvailabilityZonesForMetadata, groupAvailabilityZonesForGroup);
        stack.setNetwork(TestUtil.networkWithSubnetId(subnetId));
        stack.getInstanceGroups()
                .forEach(ig -> ig.getInstanceMetaData().addAll(getInstanceMetaData(1, List.of(), ig, Set.of())));

        when(cloudPlatformConnectors.get(any()).availabilityZoneConnector()).thenReturn(availabilityZoneConnector);
        Set<String> groupNamesToScale = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet());
        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        when(instanceMetaDataService.getNotDeletedInstanceMetadataWithNetworkByStackId(stack.getId())).thenReturn(notDeletedInstanceMetaDataSet);
        notDeletedInstanceMetaDataSet.forEach(im -> {
            if (!"test-1-1".equals(im.getInstanceId()) && "is1".equals(im.getInstanceGroup().getGroupName())) {
                im.setInstanceStatus(InstanceStatus.REQUESTED);
                im.setAvailabilityZone(null);
                im.setRackId(null);
            }
        });
        when(instanceMetaDataService.getAvailabilityZoneFromDiskIfRepair(any(), anyBoolean(), anyString(), anyString())).thenReturn(null);

        underTest.populateForScaling(stack, groupNamesToScale, repair, NetworkScaleDetails.getEmpty());

        Map<String, Long> zoneNodeCount = new HashMap<>();
        notDeletedInstanceMetaDataSet.stream().filter(im -> "is1".equals(im.getInstanceGroup().getGroupName()))
                .forEach(im -> zoneNodeCount.put(im.getAvailabilityZone(), 1 + zoneNodeCount.getOrDefault(im.getAvailabilityZone(), 0L)));
        assertEquals(Map.of("1", 1L, "2", 1L, "3", 1L), zoneNodeCount);
        Assertions.assertTrue(notDeletedInstanceMetaDataSet.stream()
                .filter(im -> InstanceStatus.REQUESTED.equals(im.getInstanceStatus()))
                .allMatch(im -> subnetId.equals(im.getSubnetId())));
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
