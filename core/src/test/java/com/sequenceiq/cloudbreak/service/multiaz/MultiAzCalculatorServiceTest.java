package com.sequenceiq.cloudbreak.service.multiaz;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
public class MultiAzCalculatorServiceTest {

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String AVAILABILITY_ZONE = "AVAILABILITY_ZONE";

    private static final int SINGLE_INSTANCE = 1;

    private static final int NO_SUBNETS = 0;

    private static final int SINGLE_SUBNET = 1;

    @Mock
    private MultiAzValidator multiAzValidator;

    @InjectMocks
    private MultiAzCalculatorService underTest;

    @Test
    public void testPrepareSubnetAzMap() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetMetas(Map.of(
                cloudSubnetName(1), cloudSubnet(1),
                cloudSubnetName(2), cloudSubnet(2)
        ));
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);

        Map<String, String> result = underTest.prepareSubnetAzMap(detailedEnvironmentResponse);

        Assertions.assertEquals(result.size(), 4);
        Assertions.assertEquals(result.get(cloudSubnetName(1)), cloudSubnetAz(1));
        Assertions.assertEquals(result.get(cloudSubnetName(2)), cloudSubnetAz(2));
    }

    @Test
    public void testPrepareMultiAzMapWhenAvailabilityZoneNotNull() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetMetas(Map.of(
                cloudSubnetName(1), cloudSubnet(1),
                cloudSubnetName(2), cloudSubnet(2)
        ));
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);

        Map<String, String> actual = underTest.prepareSubnetAzMap(detailedEnvironmentResponse, "az-1");
        Assertions.assertEquals(2, actual.size());
        Assertions.assertEquals("az-1", actual.get("subnet-1"));
        Assertions.assertEquals("az-1", actual.get("1"));
    }

    static Object[][] prepareSubnetAzMapTestWhenEmptyResultDataProvider() {
        return new Object[][]{
                // testCaseName environment
                {"environment=null", null},
                {"network=null", DetailedEnvironmentResponse.builder().withNetwork(null).build()},
                {"subnetMetas=null",
                        DetailedEnvironmentResponse.builder().withNetwork(EnvironmentNetworkResponse.builder().withSubnetMetas(null).build()).build()},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("prepareSubnetAzMapTestWhenEmptyResultDataProvider")
    void prepareSubnetAzMapTestWhenEmptyResult(String testCaseName, DetailedEnvironmentResponse environment) {
        Map<String, String> result = underTest.prepareSubnetAzMap(environment);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    static Object[][] testSubnetDistributionForWholeInstanceGroupData() {
        return new Object[][]{
                // cloudPlatform, subnetCount, instanceCount, expectedCounts, supported
                {CloudPlatform.AWS, 1, 99, Arrays.asList(99L), true},
                {CloudPlatform.AWS, 3, 99, Arrays.asList(33L, 33L, 33L), true},
                {CloudPlatform.AWS, 3, 60, Arrays.asList(20L, 20L, 20L), true},
                {CloudPlatform.AWS, 3, 59, Arrays.asList(20L, 20L, 19L), true},
                {CloudPlatform.AWS, 3, 58, Arrays.asList(20L, 19L, 19L), true},
                {CloudPlatform.AWS, 3, 57, Arrays.asList(19L, 19L, 19L), true},
                {CloudPlatform.AWS, 3, 56, Arrays.asList(19L, 19L, 18L), true},
                {CloudPlatform.AWS, 4, 60, Arrays.asList(15L, 15L, 15L, 15L), true},
                {CloudPlatform.AWS, 4, 59, Arrays.asList(15L, 15L, 15L, 14L), true},
                {CloudPlatform.AWS, 4, 58, Arrays.asList(15L, 15L, 14L, 14L), true},
                {CloudPlatform.AWS, 4, 57, Arrays.asList(15L, 14L, 14L, 14L), true},
                {CloudPlatform.AWS, 4, 56, Arrays.asList(14L, 14L, 14L, 14L), true},
                {CloudPlatform.YARN, 0, 0, Arrays.asList(), false},
        };
    }

    @ParameterizedTest(name = "testSubnetDistributionForWholeInstanceGroup " +
            "with {0} platform when {1} subnets and {2} instances should result in {3} subnet counts")
    @MethodSource("testSubnetDistributionForWholeInstanceGroupData")
    public void testSubnetDistributionForWholeInstanceGroup(CloudPlatform cloudPlatform, int subnetCount, int instanceCount, List<Long> expectedCounts,
            boolean supported) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupNetwork.class))).thenReturn(true);
        }
        InstanceGroupDto instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount), ((InstanceGroup) instanceGroup.getInstanceGroup()).getInstanceGroupNetwork(),
                ((InstanceGroup) instanceGroup.getInstanceGroup()).getNotDeletedAndNotZombieInstanceMetaDataSet());

        List<Long> actualCounts = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            int finalI = i;
            actualCounts.add(instanceGroup.getInstanceMetadataViews()
                    .stream()
                    .filter(instance -> instance.getSubnetId().equals(cloudSubnetName(finalI)))
                    .count());
        }
        Collections.sort(expectedCounts);
        Collections.sort(actualCounts);

        Assertions.assertEquals(expectedCounts, actualCounts);
    }

    @ParameterizedTest(name = "testSubnetDistributionForWholeInstanceGroupWhenPrepopulatedForeignSubnetId " +
            "with {0} platform when {1} subnets and {2} instances should result in {3} subnet counts")
    @MethodSource("testSubnetDistributionForWholeInstanceGroupData")
    public void testSubnetDistributionForWholeInstanceGroupWhenPrepopulatedForeignSubnetId(CloudPlatform cloudPlatform, int subnetCount, int instanceCount,
            List<Long> expectedCounts, boolean supported) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupNetwork.class))).thenReturn(true);
        }

        InstanceGroupDto instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        InstanceMetaData instanceWithPrepopulatedForeignSubnetId = instanceMetaData(instanceCount, SUBNET_ID, AVAILABILITY_ZONE, InstanceStatus.REQUESTED);
        instanceGroup.getInstanceMetadataViews().add(instanceWithPrepopulatedForeignSubnetId);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount), ((InstanceGroup) instanceGroup.getInstanceGroup()).getInstanceGroupNetwork(),
                ((InstanceGroup) instanceGroup.getInstanceGroup()).getNotDeletedAndNotZombieInstanceMetaDataSet());

        List<Long> actualCounts = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            int finalI = i;
            actualCounts.add(instanceGroup.getInstanceMetadataViews()
                    .stream()
                    .filter(instance -> instance.getSubnetId().equals(cloudSubnetName(finalI)))
                    .count());
        }
        Collections.sort(expectedCounts);
        Collections.sort(actualCounts);

        Assertions.assertEquals(expectedCounts, actualCounts);

        assertThat(instanceWithPrepopulatedForeignSubnetId.getSubnetId()).isEqualTo(SUBNET_ID);
        assertThat(instanceWithPrepopulatedForeignSubnetId.getAvailabilityZone()).isEqualTo(AVAILABILITY_ZONE);
    }

    static Object[][] testSubnetDistributionForWholeInstanceGroupWhen5InstancesWithPrepopulatedKnownSubnetIdData() {
        return new Object[][]{
                // cloudPlatform, subnetCount, instanceCount, expectedCounts, supported
                {CloudPlatform.AWS, 1, 99, Arrays.asList(104L), true},
                {CloudPlatform.AWS, 3, 99, Arrays.asList(35L, 35L, 34L), true},
                {CloudPlatform.AWS, 3, 60, Arrays.asList(22L, 22L, 21L), true},
                {CloudPlatform.AWS, 3, 59, Arrays.asList(22L, 21L, 21L), true},
                {CloudPlatform.AWS, 3, 58, Arrays.asList(21L, 21L, 21L), true},
                {CloudPlatform.AWS, 3, 57, Arrays.asList(21L, 21L, 20L), true},
                {CloudPlatform.AWS, 3, 56, Arrays.asList(21L, 20L, 20L), true},
                {CloudPlatform.AWS, 4, 60, Arrays.asList(17L, 16L, 16L, 16L), true},
                {CloudPlatform.AWS, 4, 59, Arrays.asList(16L, 16L, 16L, 16L), true},
                {CloudPlatform.AWS, 4, 58, Arrays.asList(16L, 16L, 16L, 15L), true},
                {CloudPlatform.AWS, 4, 57, Arrays.asList(16L, 16L, 15L, 15L), true},
                {CloudPlatform.AWS, 4, 56, Arrays.asList(16L, 15L, 15L, 15L), true},
                {CloudPlatform.YARN, 0, 0, Arrays.asList(), false},
        };
    }

    @ParameterizedTest(name = "testSubnetDistributionForWholeInstanceGroupWhen5InstancesWithPrepopulatedKnownSubnetId " +
            "with {0} platform when {1} subnets and {2} instances should result in {3} subnet counts")
    @MethodSource("testSubnetDistributionForWholeInstanceGroupWhen5InstancesWithPrepopulatedKnownSubnetIdData")
    public void testSubnetDistributionForWholeInstanceGroupWhen5InstancesWithPrepopulatedKnownSubnetId(CloudPlatform cloudPlatform, int subnetCount,
            int instanceCount, List<Long> expectedCounts, boolean supported) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupNetwork.class))).thenReturn(true);
        }

        InstanceGroupDto instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        Set<InstanceMetaData> extraInstancesWithPrepopulatedSubnetId = new HashSet<>();
        String subnetIdForExtraInstances = cloudSubnetName(0);
        String availabilityZoneForExtraInstances = cloudSubnetAz(0);
        if (subnetCount > 0) {
            for (int i = 0; i < 5; i++) {
                extraInstancesWithPrepopulatedSubnetId.add(instanceMetaData(instanceCount + i, subnetIdForExtraInstances, availabilityZoneForExtraInstances,
                        null));
            }
        }
        instanceGroup.addAllInstanceMetadata(extraInstancesWithPrepopulatedSubnetId);

        // until the InstanceGroups are not refactored completly, we need the cast
        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount), ((InstanceGroup) instanceGroup.getInstanceGroup()).getInstanceGroupNetwork(),
                ((InstanceGroup) instanceGroup.getInstanceGroup()).getNotDeletedAndNotZombieInstanceMetaDataSet());

        List<Long> actualCounts = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            int finalI = i;
            actualCounts.add(instanceGroup.getInstanceMetadataViews()
                    .stream()
                    .filter(instance -> instance.getSubnetId().equals(cloudSubnetName(finalI)))
                    .count());
        }
        Collections.sort(expectedCounts);
        Collections.sort(actualCounts);

        Assertions.assertEquals(expectedCounts, actualCounts);

        extraInstancesWithPrepopulatedSubnetId.forEach(instance -> {
            assertThat(instance.getSubnetId()).isEqualTo(subnetIdForExtraInstances);
            assertThat(instance.getAvailabilityZone()).isEqualTo(availabilityZoneForExtraInstances);
        });
    }

    @ParameterizedTest(name = "testSubnetDistributionForWholeInstanceGroupWhenDeletedInstances " +
            "with {0} platform when {1} subnets and {2} instances should result in {3} subnet counts")
    @MethodSource("testSubnetDistributionForWholeInstanceGroupData")
    public void testSubnetDistributionForWholeInstanceGroupWhenDeletedInstances(CloudPlatform cloudPlatform, int subnetCount, int instanceCount,
            List<Long> expectedCounts, boolean supported) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupNetwork.class))).thenReturn(true);
        }

        InstanceGroupDto instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        Set<InstanceMetaData> deletedInstancesWithPrepopulatedSubnetId = new HashSet<>();
        String subnetIdForDeletedInstances = cloudSubnetName(0);
        String availabilityZoneForDeletedInstances = cloudSubnetAz(0);
        if (subnetCount > 0) {
            InstanceMetaData instanceWithTerminationDate = instanceMetaData(instanceCount, subnetIdForDeletedInstances, availabilityZoneForDeletedInstances,
                    null);
            instanceWithTerminationDate.setTerminationDate(1234L);
            deletedInstancesWithPrepopulatedSubnetId.add(instanceWithTerminationDate);
            deletedInstancesWithPrepopulatedSubnetId.add(instanceMetaData(instanceCount + 1, subnetIdForDeletedInstances, availabilityZoneForDeletedInstances,
                    InstanceStatus.TERMINATED));
            deletedInstancesWithPrepopulatedSubnetId.add(instanceMetaData(instanceCount + 2, subnetIdForDeletedInstances, availabilityZoneForDeletedInstances,
                    InstanceStatus.DELETED_BY_PROVIDER));
            deletedInstancesWithPrepopulatedSubnetId.add(instanceMetaData(instanceCount + 3, subnetIdForDeletedInstances, availabilityZoneForDeletedInstances,
                    InstanceStatus.DELETED_ON_PROVIDER_SIDE));
        }
        instanceGroup.getInstanceMetadataViews().addAll(deletedInstancesWithPrepopulatedSubnetId);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount), ((InstanceGroup) instanceGroup.getInstanceGroup()).getInstanceGroupNetwork(),
                ((InstanceGroup) instanceGroup.getInstanceGroup()).getNotDeletedAndNotZombieInstanceMetaDataSet());

        List<Long> actualCounts = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            int finalI = i;
            actualCounts.add(instanceGroup.getInstanceMetadataViews()
                    .stream()
                    .filter(instance -> instance.getSubnetId().equals(cloudSubnetName(finalI)))
                    .filter(instance -> !deletedInstancesWithPrepopulatedSubnetId.contains(instance))
                    .count());
        }
        Collections.sort(expectedCounts);
        Collections.sort(actualCounts);

        Assertions.assertEquals(expectedCounts, actualCounts);

        deletedInstancesWithPrepopulatedSubnetId.forEach(instance -> {
            assertThat(instance.getSubnetId()).isEqualTo(subnetIdForDeletedInstances);
            assertThat(instance.getAvailabilityZone()).isEqualTo(availabilityZoneForDeletedInstances);
        });
    }

    static Object[][] calculateByRoundRobinTestWhenWholeInstanceGroupAndNoSubnetIdsDataProvider() {
        return new Object[][]{
                // testCaseName disableNetwork disableAttributes disableSubnetIdsAttribute
                {"instanceGroupNetwork=null", true, false, false},
                {"attributes=null", false, true, false},
                {"subnetIdsAttribute=null", false, false, true},
                {"subnetIds=empty", false, false, false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("calculateByRoundRobinTestWhenWholeInstanceGroupAndNoSubnetIdsDataProvider")
    void calculateByRoundRobinTestWhenWholeInstanceGroupAndNoSubnetIds(String testCaseName, boolean disableNetwork, boolean disableAttributes,
            boolean disableSubnetIdsAttribute) {
        InstanceGroupDto instanceGroupDto = instanceGroup(CloudPlatform.AWS, SINGLE_INSTANCE, NO_SUBNETS);
        InstanceGroup instanceGroup = (InstanceGroup) instanceGroupDto.getInstanceGroup();
        if (disableNetwork) {
            instanceGroup.setInstanceGroupNetwork(null);
        } else if (disableAttributes) {
            instanceGroup.getInstanceGroupNetwork().setAttributes(null);
        } else if (disableSubnetIdsAttribute) {
            instanceGroup.getInstanceGroupNetwork().setAttributes(new Json(Map.of()));
        }

        underTest.calculateByRoundRobin(
                subnetAzPairs(NO_SUBNETS), instanceGroup.getInstanceGroupNetwork(),
                (instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet()));

        instanceGroupDto.getInstanceMetadataViews().forEach(instance -> {
            assertThat(instance.getSubnetId()).isNull();
            assertThat(instance.getAvailabilityZone()).isNull();
        });
    }

    static Object[][] calculateByRoundRobinTestWhenCloudInstanceData() {
        return new Object[][]{
                // cloudPlatform, subnetCount, instanceCount, existingCounts, supported, expectedPermissibleSubnetIdIndexes
                {CloudPlatform.YARN, 0, 0, List.of(), false, Set.of()},
                {CloudPlatform.AWS, 1, 0, List.of(), true, Set.of(0)},
                {CloudPlatform.AWS, 1, 1, List.of(1), true, Set.of(0)},
                {CloudPlatform.AWS, 1, 2, List.of(2), true, Set.of(0)},
                {CloudPlatform.AWS, 2, 0, List.of(), true, Set.of(0, 1)},
                {CloudPlatform.AWS, 2, 1, List.of(1, 0), true, Set.of(1)},
                {CloudPlatform.AWS, 2, 1, List.of(0, 1), true, Set.of(0)},
                {CloudPlatform.AWS, 2, 2, List.of(2, 0), true, Set.of(1)},
                {CloudPlatform.AWS, 2, 2, List.of(1, 1), true, Set.of(0, 1)},
                {CloudPlatform.AWS, 2, 2, List.of(0, 2), true, Set.of(0)},
                {CloudPlatform.AWS, 2, 3, List.of(3, 0), true, Set.of(1)},
                {CloudPlatform.AWS, 2, 3, List.of(0, 3), true, Set.of(0)},
                {CloudPlatform.AWS, 2, 3, List.of(2, 1), true, Set.of(1)},
                {CloudPlatform.AWS, 2, 3, List.of(1, 2), true, Set.of(0)},
                {CloudPlatform.AWS, 3, 0, List.of(), true, Set.of(0, 1, 2)},
                {CloudPlatform.AWS, 3, 1, List.of(1, 0, 0), true, Set.of(1, 2)},
                {CloudPlatform.AWS, 3, 1, List.of(0, 1, 0), true, Set.of(0, 2)},
                {CloudPlatform.AWS, 3, 1, List.of(0, 0, 1), true, Set.of(0, 1)},
                {CloudPlatform.AWS, 3, 2, List.of(2, 0, 0), true, Set.of(1, 2)},
                {CloudPlatform.AWS, 3, 2, List.of(0, 2, 0), true, Set.of(0, 2)},
                {CloudPlatform.AWS, 3, 2, List.of(0, 0, 2), true, Set.of(0, 1)},
                {CloudPlatform.AWS, 3, 2, List.of(1, 1, 0), true, Set.of(2)},
                {CloudPlatform.AWS, 3, 2, List.of(0, 1, 1), true, Set.of(0)},
                {CloudPlatform.AWS, 3, 2, List.of(1, 0, 1), true, Set.of(1)},
                {CloudPlatform.AWS, 3, 3, List.of(3, 0, 0), true, Set.of(1, 2)},
                {CloudPlatform.AWS, 3, 3, List.of(0, 3, 0), true, Set.of(0, 2)},
                {CloudPlatform.AWS, 3, 3, List.of(0, 0, 3), true, Set.of(0, 1)},
                {CloudPlatform.AWS, 3, 3, List.of(2, 1, 0), true, Set.of(2)},
                {CloudPlatform.AWS, 3, 3, List.of(0, 1, 2), true, Set.of(0)},
                {CloudPlatform.AWS, 3, 3, List.of(2, 0, 1), true, Set.of(1)},
                {CloudPlatform.AWS, 3, 3, List.of(1, 1, 1), true, Set.of(0, 1, 2)},
                {CloudPlatform.AWS, 3, 4, List.of(4, 0, 0), true, Set.of(1, 2)},
                {CloudPlatform.AWS, 3, 4, List.of(0, 4, 0), true, Set.of(0, 2)},
                {CloudPlatform.AWS, 3, 4, List.of(0, 0, 4), true, Set.of(0, 1)},
                {CloudPlatform.AWS, 3, 4, List.of(3, 1, 0), true, Set.of(2)},
                {CloudPlatform.AWS, 3, 4, List.of(1, 3, 0), true, Set.of(2)},
                {CloudPlatform.AWS, 3, 4, List.of(3, 0, 1), true, Set.of(1)},
                {CloudPlatform.AWS, 3, 4, List.of(1, 0, 3), true, Set.of(1)},
                {CloudPlatform.AWS, 3, 4, List.of(0, 3, 1), true, Set.of(0)},
                {CloudPlatform.AWS, 3, 4, List.of(0, 1, 3), true, Set.of(0)},
                {CloudPlatform.AWS, 3, 4, List.of(2, 2, 0), true, Set.of(2)},
                {CloudPlatform.AWS, 3, 4, List.of(2, 0, 2), true, Set.of(1)},
                {CloudPlatform.AWS, 3, 4, List.of(0, 2, 2), true, Set.of(0)},
                {CloudPlatform.AWS, 3, 4, List.of(2, 1, 1), true, Set.of(1, 2)},
                {CloudPlatform.AWS, 3, 4, List.of(1, 2, 1), true, Set.of(0, 2)},
                {CloudPlatform.AWS, 3, 4, List.of(1, 1, 2), true, Set.of(0, 1)},
        };
    }

    @ParameterizedTest(name = "calculateByRoundRobinTestWhenCloudInstance " +
            "with {0} platform when {1} subnets and {2} instances and {3} subnet counts should result in {5} subnet / AZ")
    @MethodSource("calculateByRoundRobinTestWhenCloudInstanceData")
    public void calculateByRoundRobinTestWhenCloudInstance(CloudPlatform cloudPlatform, int subnetCount, int instanceCount, List<Integer> existingCounts,
            boolean supported, Set<Integer> expectedPermissibleSubnetIdIndexes) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        }

        InstanceGroupDto instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        initSubnetIdAndAvailabilityZoneForInstances(existingCounts, instanceGroup);

        InstanceMetaData instanceMetaData = new InstanceMetaData();

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup,
                instanceMetaData,
                NetworkScaleDetails.getEmpty());

        verifyCloudInstance(expectedPermissibleSubnetIdIndexes, instanceMetaData);
        verifySubnetIdAndAvailabilityZoneForInstancesAreUnchanged(existingCounts, instanceGroup, Set.of());
    }

    @ParameterizedTest(name = "calculateByRoundRobinTestWhenCloudInstanceAndForeignSubnetId " +
            "with {0} platform when {1} subnets and {2} instances and {3} subnet counts should result in {5} subnet / AZ")
    @MethodSource("calculateByRoundRobinTestWhenCloudInstanceData")
    public void calculateByRoundRobinTestWhenCloudInstanceAndForeignSubnetId(CloudPlatform cloudPlatform, int subnetCount, int instanceCount,
            List<Integer> existingCounts, boolean supported, Set<Integer> expectedPermissibleSubnetIdIndexes) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        }

        InstanceGroupDto instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        initSubnetIdAndAvailabilityZoneForInstances(existingCounts, instanceGroup);
        InstanceMetaData instanceWithPrepopulatedForeignSubnetId = instanceMetaData(instanceCount, SUBNET_ID, AVAILABILITY_ZONE, InstanceStatus.REQUESTED);
        instanceGroup.getInstanceMetadataViews().add(instanceWithPrepopulatedForeignSubnetId);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup,
                instanceMetaData,
                NetworkScaleDetails.getEmpty());

        verifyCloudInstance(expectedPermissibleSubnetIdIndexes, instanceMetaData);
        verifySubnetIdAndAvailabilityZoneForInstancesAreUnchanged(existingCounts, instanceGroup, Set.of(instanceWithPrepopulatedForeignSubnetId));

        assertThat(instanceWithPrepopulatedForeignSubnetId.getSubnetId()).isEqualTo(SUBNET_ID);
        assertThat(instanceWithPrepopulatedForeignSubnetId.getAvailabilityZone()).isEqualTo(AVAILABILITY_ZONE);
    }

    @ParameterizedTest(name = "calculateByRoundRobinTestWhenCloudInstanceAndDeletedInstances " +
            "with {0} platform when {1} subnets and {2} instances and {3} subnet counts should result in {5} subnet / AZ")
    @MethodSource("calculateByRoundRobinTestWhenCloudInstanceData")
    public void calculateByRoundRobinTestWhenCloudInstanceAndDeletedInstances(CloudPlatform cloudPlatform, int subnetCount, int instanceCount,
            List<Integer> existingCounts, boolean supported, Set<Integer> expectedPermissibleSubnetIdIndexes) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupView.class))).thenReturn(true);
        }

        InstanceGroupDto instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        initSubnetIdAndAvailabilityZoneForInstances(existingCounts, instanceGroup);
        Set<InstanceMetaData> deletedInstancesWithPrepopulatedSubnetId = new HashSet<>();
        String subnetIdForDeletedInstances = cloudSubnetName(0);
        String availabilityZoneForDeletedInstances = cloudSubnetAz(0);
        if (subnetCount > 0) {
            InstanceMetaData instanceWithTerminationDate = instanceMetaData(instanceCount, subnetIdForDeletedInstances, availabilityZoneForDeletedInstances,
                    null);
            instanceWithTerminationDate.setTerminationDate(1234L);
            deletedInstancesWithPrepopulatedSubnetId.add(instanceWithTerminationDate);
            deletedInstancesWithPrepopulatedSubnetId.add(instanceMetaData(instanceCount + 1, subnetIdForDeletedInstances, availabilityZoneForDeletedInstances,
                    InstanceStatus.ZOMBIE));
            deletedInstancesWithPrepopulatedSubnetId.add(instanceMetaData(instanceCount + 2, subnetIdForDeletedInstances, availabilityZoneForDeletedInstances,
                    InstanceStatus.DELETED_BY_PROVIDER));
            deletedInstancesWithPrepopulatedSubnetId.add(instanceMetaData(instanceCount + 3, subnetIdForDeletedInstances, availabilityZoneForDeletedInstances,
                    InstanceStatus.DELETED_ON_PROVIDER_SIDE));
        }
        instanceGroup.getInstanceMetadataViews().addAll(deletedInstancesWithPrepopulatedSubnetId);

        InstanceMetaData instanceMetaData = new InstanceMetaData();

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup,
                instanceMetaData,
                NetworkScaleDetails.getEmpty());

        verifyCloudInstance(expectedPermissibleSubnetIdIndexes, instanceMetaData);
        verifySubnetIdAndAvailabilityZoneForInstancesAreUnchanged(existingCounts, instanceGroup, deletedInstancesWithPrepopulatedSubnetId);

        deletedInstancesWithPrepopulatedSubnetId.forEach(instance -> {
            assertThat(instance.getSubnetId()).isEqualTo(subnetIdForDeletedInstances);
            assertThat(instance.getAvailabilityZone()).isEqualTo(availabilityZoneForDeletedInstances);
        });
    }

    static Object[][] calculateByRoundRobinTestWhenCloudInstanceAndNoSubnetIdsDataProvider() {
        return new Object[][]{
                // testCaseName disableNetwork disableAttributes disableSubnetIdsAttribute prepopulateCloudInstanceSubnet
                {"instanceGroupNetwork=null, prepopulateCloudInstanceSubnet=false", true, false, false, false},
                {"instanceGroupNetwork=null, prepopulateCloudInstanceSubnet=true", true, false, false, true},
                {"attributes=null, prepopulateCloudInstanceSubnet=false", false, true, false, false},
                {"attributes=null, prepopulateCloudInstanceSubnet=true", false, true, false, true},
                {"subnetIdsAttribute=null, prepopulateCloudInstanceSubnet=false", false, false, true, false},
                {"subnetIdsAttribute=null, prepopulateCloudInstanceSubnet=true", false, false, true, true},
                {"subnetIds=empty, prepopulateCloudInstanceSubnet=false", false, false, false, false},
                {"subnetIds=empty, prepopulateCloudInstanceSubnet=true", false, false, false, true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("calculateByRoundRobinTestWhenCloudInstanceAndNoSubnetIdsDataProvider")
    void calculateByRoundRobinTestWhenCloudInstanceAndNoSubnetIds(String testCaseName, boolean disableNetwork, boolean disableAttributes,
            boolean disableSubnetIdsAttribute, boolean prepopulateCloudInstanceSubnet) {
        InstanceGroupDto instanceGroup = instanceGroup(CloudPlatform.AWS, SINGLE_INSTANCE, NO_SUBNETS);
        if (disableNetwork) {
            ((InstanceGroup) instanceGroup.getInstanceGroup()).setInstanceGroupNetwork(null);
        } else if (disableAttributes) {
            ((InstanceGroup) instanceGroup.getInstanceGroup()).getInstanceGroupNetwork().setAttributes(null);
        } else if (disableSubnetIdsAttribute) {
            ((InstanceGroup) instanceGroup.getInstanceGroup()).getInstanceGroupNetwork().setAttributes(new Json(Map.of()));
        }

        String cloudInstanceSubnetId = prepopulateCloudInstanceSubnet ? SUBNET_ID : null;
        String cloudInstanceAvailabilityZone = prepopulateCloudInstanceSubnet ? AVAILABILITY_ZONE : null;
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setSubnetId(cloudInstanceSubnetId);
        instanceMetaData.setAvailabilityZone(cloudInstanceAvailabilityZone);

        underTest.calculateByRoundRobin(
                subnetAzPairs(NO_SUBNETS),
                instanceGroup,
                instanceMetaData,
                NetworkScaleDetails.getEmpty());

        verifyCloudInstance(cloudInstanceSubnetId == null ? Set.of() : Set.of(cloudInstanceSubnetId),
                cloudInstanceAvailabilityZone == null ? Set.of() : Set.of(cloudInstanceAvailabilityZone),
                instanceMetaData);

        instanceGroup.getInstanceMetadataViews().forEach(instance -> {
            assertThat(instance.getSubnetId()).isNull();
            assertThat(instance.getAvailabilityZone()).isNull();
        });
    }

    @Test
    public void calculateByRoundRobinTestWhenCloudInstanceAndPrepopulatedCloudInstanceSubnet() {
        when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);

        List<Integer> existingCounts = List.of(SINGLE_SUBNET);
        InstanceGroupDto instanceGroup = instanceGroup(CloudPlatform.AWS, SINGLE_INSTANCE, SINGLE_SUBNET);
        initSubnetIdAndAvailabilityZoneForInstances(existingCounts, instanceGroup);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setSubnetId(SUBNET_ID);
        instanceMetaData.setAvailabilityZone(AVAILABILITY_ZONE);

        underTest.calculateByRoundRobin(
                subnetAzPairs(SINGLE_SUBNET),
                instanceGroup,
                instanceMetaData,
                NetworkScaleDetails.getEmpty());

        verifyCloudInstance(Set.of(SUBNET_ID), Set.of(AVAILABILITY_ZONE), instanceMetaData);
        verifySubnetIdAndAvailabilityZoneForInstancesAreUnchanged(existingCounts, instanceGroup, Set.of());
    }

    static Object[][] calculateByRoundRobinTestWhenCloudInstanceAndInstancesWithoutSubnetData() {
        return new Object[][]{
                // cloudPlatform, subnetCount, instanceCount, supported, expectedPermissibleSubnetIdIndexes
                {CloudPlatform.YARN, 0, 0, false, Set.of()},
                {CloudPlatform.AWS, 1, 0, true, Set.of(0)},
                {CloudPlatform.AWS, 1, 1, true, Set.of(0)},
                {CloudPlatform.AWS, 1, 2, true, Set.of(0)},
                {CloudPlatform.AWS, 2, 0, true, Set.of(0, 1)},
                {CloudPlatform.AWS, 2, 1, true, Set.of(0, 1)},
                {CloudPlatform.AWS, 2, 2, true, Set.of(0, 1)},
                {CloudPlatform.AWS, 2, 3, true, Set.of(0, 1)},
                {CloudPlatform.AWS, 3, 0, true, Set.of(0, 1, 2)},
                {CloudPlatform.AWS, 3, 1, true, Set.of(0, 1, 2)},
                {CloudPlatform.AWS, 3, 2, true, Set.of(0, 1, 2)},
                {CloudPlatform.AWS, 3, 3, true, Set.of(0, 1, 2)},
                {CloudPlatform.AWS, 3, 4, true, Set.of(0, 1, 2)},
        };
    }

    @ParameterizedTest(name = "calculateByRoundRobinTestWhenCloudInstanceAndInstancesWithoutSubnet " +
            "with {0} platform when {1} subnets and {2} instances and {3} subnet counts should result in {5} subnet / AZ")
    @MethodSource("calculateByRoundRobinTestWhenCloudInstanceAndInstancesWithoutSubnetData")
    public void calculateByRoundRobinTestWhenCloudInstanceAndInstancesWithoutSubnet(CloudPlatform cloudPlatform, int subnetCount, int instanceCount,
            boolean supported, Set<Integer> expectedPermissibleSubnetIdIndexes) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        }

        InstanceGroupDto instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);

        InstanceMetaData instanceMetaData = new InstanceMetaData();

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup,
                instanceMetaData,
                NetworkScaleDetails.getEmpty());

        verifyCloudInstance(expectedPermissibleSubnetIdIndexes, instanceMetaData);

        instanceGroup.getInstanceMetadataViews().forEach(instance -> {
            assertThat(instance.getSubnetId()).isNull();
            assertThat(instance.getAvailabilityZone()).isNull();
        });
    }

    @Test
    public void calculateByRoundRobinTestWhenSubnetAndAvailabilityZoneAndRackIdAndStackFallback() throws IOException {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetMetas(Map.of(
                cloudSubnetName(1), cloudSubnet(1),
                cloudSubnetName(2), cloudSubnet(2),
                cloudSubnetName(3), cloudSubnet(3)
        ));
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);

        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        network.setCloudPlatform("aws");
        network.setAttributes(Json.silent(Map.of(SUBNET_IDS, List.of("subnet-1", "subnet-2", "subnet-3"))));
        InstanceGroup workerGroup = getARequestGroup("worker", 3, InstanceGroupType.CORE);
        workerGroup.setInstanceGroupNetwork(network);
        stack.setInstanceGroups(Set.of(workerGroup));
        when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroupNetwork.class))).thenReturn(true);
        Map<String, String> subnetAzPairs = underTest.prepareSubnetAzMap(detailedEnvironmentResponse);

        underTest.calculateByRoundRobin(subnetAzPairs, stack);

        Map<String, Set<InstanceMetaData>> hostGroupInstances = stack.getInstanceGroups().stream().collect(
                Collectors.toMap(InstanceGroup::getGroupName, InstanceGroup::getAllInstanceMetaData));

        long privateIdStart = 0L;
        validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId("worker", 3, hostGroupInstances.get("worker"),
                List.of("subnet-1", "subnet-2", "subnet-3"),
                List.of("az-1", "az-2", "az-3"),
                List.of("/az-1", "/az-2", "/az-3"));
    }

    private void validateInstanceMetadataSubnetAndAvailabilityZoneAndRackId(String hostGroup, int nodeCount, Set<InstanceMetaData> instanceMetaData,
            List<String> subnetIdExpected, List<String> availabilityZoneExpected, List<String> rackIdExpected) {
        assertEquals(nodeCount, instanceMetaData.size(), "Instance Metadata size should match for hostgroup: " + hostGroup);
        for (InstanceMetaData im : instanceMetaData) {
            assertThat(subnetIdExpected).overridingErrorMessage("Subnet Id should match for hostgroup: " + hostGroup).contains(im.getSubnetId());
            assertThat(availabilityZoneExpected).overridingErrorMessage("Availability Zone should match for hostgroup: " + hostGroup)
                    .contains(im.getAvailabilityZone());
            assertThat(rackIdExpected).overridingErrorMessage("Rack Id should match for hostgroup: " + hostGroup).contains(im.getRackId());
        }
    }

    static Object[][] determineRackIdTestDataProvider() {
        return new Object[][]{
                // testCaseName subnetId availabilityZone rackIdExpected
                {"subnetId=null, availabilityZone=null", null, null, "/default-rack"},
                {"subnetId=\"\", availabilityZone=null", "", null, "/default-rack"},
                {"subnetId=null, availabilityZone=\"\"", null, "", "/default-rack"},
                {"subnetId=\"\", availabilityZone=\"\"", "", "", "/default-rack"},
                {"subnetId=SUBNET_ID, availabilityZone=null", SUBNET_ID, null, "/SUBNET_ID"},
                {"subnetId=SUBNET_ID, availabilityZone=\"\"", SUBNET_ID, "", "/SUBNET_ID"},
                {"subnetId=null, availabilityZone=AVAILABILITY_ZONE", null, AVAILABILITY_ZONE, "/AVAILABILITY_ZONE"},
                {"subnetId=\"\", availabilityZone=AVAILABILITY_ZONE", "", AVAILABILITY_ZONE, "/AVAILABILITY_ZONE"},
                {"subnetId=SUBNET_ID, availabilityZone=AVAILABILITY_ZONE", SUBNET_ID, AVAILABILITY_ZONE, "/AVAILABILITY_ZONE"},
        };
    }

    @ParameterizedTest(name = "saveInstanceMetaDataTestDataProvider {0}")
    @MethodSource("determineRackIdTestDataProvider")
    void determineRackIdTest(String testCaseName, String subnetId, String availabilityZone, String rackIdExpected) {
        String rackId = underTest.determineRackId(subnetId, availabilityZone);

        assertThat(rackId).isEqualTo(rackIdExpected);
    }

    static Object[][] calculateByRoundRobinTestWhenCloudInstanceDataAndPreferredAvailabilityZones() {
        return new Object[][]{
                // cloudPlatform, subnetCount, instanceCount, existingCounts, supported, expectedPermissibleSubnetIdIndexes, PreferredSubnetIdIndexes
                {CloudPlatform.AWS, 1, 0, List.of(), true, Set.of(0), Set.of()},
                {CloudPlatform.AWS, 1, 1, List.of(1), true, Set.of(0), Set.of(0)},
                {CloudPlatform.AWS, 1, 2, List.of(2), true, Set.of(0), Set.of(0)},
                {CloudPlatform.AWS, 2, 2, List.of(1, 1), true, Set.of(0, 1), Set.of()},
                {CloudPlatform.AWS, 2, 2, List.of(1, 1), true, Set.of(1), Set.of(1)},
                {CloudPlatform.AWS, 3, 0, List.of(), true, Set.of(0, 2), Set.of(0, 2)},
                {CloudPlatform.AWS, 3, 1, List.of(1, 0, 0), true, Set.of(1, 2), Set.of(1, 2)},
                {CloudPlatform.AWS, 3, 1, List.of(0, 1, 0), true, Set.of(2), Set.of(2)},
                {CloudPlatform.AWS, 3, 1, List.of(0, 0, 1), true, Set.of(0), Set.of(0)},
                {CloudPlatform.AWS, 3, 2, List.of(2, 0, 0), true, Set.of(1, 2), Set.of()},
                {CloudPlatform.AWS, 3, 4, List.of(4, 0, 0), true, Set.of(0), Set.of(0)},
                {CloudPlatform.AWS, 3, 4, List.of(0, 4, 0), true, Set.of(2), Set.of(1, 2)},
                {CloudPlatform.AWS, 3, 4, List.of(0, 0, 4), true, Set.of(0), Set.of(0, 2)},
                {CloudPlatform.AWS, 3, 4, List.of(2, 1, 1), true, Set.of(1, 2), Set.of(1, 2)},
                {CloudPlatform.AWS, 3, 4, List.of(1, 2, 1), true, Set.of(2), Set.of(2)},
                {CloudPlatform.AWS, 3, 4, List.of(1, 1, 2), true, Set.of(2), Set.of(2)},
        };
    }

    @ParameterizedTest(name = "calculateByRoundRobinTestWhenCloudInstance " +
            "with {0} platform when {1} subnets, {2} instances, {3} subnet counts and preferred subnets {6} should result in {5} subnet / AZ")
    @MethodSource("calculateByRoundRobinTestWhenCloudInstanceDataAndPreferredAvailabilityZones")
    public void calculateByRoundRobinTestWhenCloudInstanceAndPreferredAvailabilityZones(CloudPlatform cloudPlatform, int subnetCount, int instanceCount,
            List<Integer> existingCounts, boolean supported, Set<Integer> expectedPermissibleSubnetIdIndexes, Set<Integer> preferredSubnetIdIndexes) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        }

        InstanceGroupDto instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        initSubnetIdAndAvailabilityZoneForInstances(existingCounts, instanceGroup);

        InstanceMetaData instanceMetaData = new InstanceMetaData();

        List<String> preferredSubnetIds = preferredSubnetIdIndexes.stream()
                .map(index -> cloudSubnet(index).getName())
                .collect(Collectors.toList());
        NetworkScaleDetails networkScaleDetails = new NetworkScaleDetails(preferredSubnetIds);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup,
                instanceMetaData,
                networkScaleDetails);

        verifyCloudInstance(expectedPermissibleSubnetIdIndexes, instanceMetaData);
        verifySubnetIdAndAvailabilityZoneForInstancesAreUnchanged(existingCounts, instanceGroup, Set.of());
    }

    @Test
    public void testWhenNoAvailableSubnetInTheEnvironmentByInstanceGroup() {
        when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        InstanceGroupDto instanceGroup = instanceGroup(CloudPlatform.AWS, SINGLE_INSTANCE, SINGLE_SUBNET);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setSubnetId(SUBNET_ID);
        instanceMetaData.setAvailabilityZone(AVAILABILITY_ZONE);
        CloudbreakServiceException actual = Assertions.assertThrows(CloudbreakServiceException.class, () ->
                underTest.calculateByRoundRobin(Map.of("anysubnet", "anyaz"), instanceGroup, instanceMetaData, NetworkScaleDetails.getEmpty()));
        assertThat(actual.getMessage()).isEqualTo("The following subnets are missing from the Environment, you may have removed them during an environment " +
                "update previously? Missing subnets: [subnet-0]");
    }

    private InstanceGroupDto instanceGroup(CloudPlatform cloudPlatform, int instanceNumber, int subnetCount) {
        InstanceGroup instanceGroup = new InstanceGroup();
        List<String> subnets = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            instanceGroup.getAvailabilityZones().add(cloudSubnetAz(i));
            subnets.add(cloudSubnetName(i));
        }
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform(cloudPlatform.name());
        instanceGroupNetwork.setAttributes(new Json(Map.of(NetworkConstants.SUBNET_IDS, subnets)));
        instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        for (int i = 0; i < instanceNumber; i++) {
            instanceGroup.getAllInstanceMetaData().add(instanceMetaData(i));
        }
        return new InstanceGroupDto(instanceGroup, new ArrayList<>(instanceGroup.getAllInstanceMetaData()));
    }

    private InstanceMetaData instanceMetaData(int i, String subnetId, String availabilityZone, InstanceStatus instanceStatus) {
        InstanceMetaData instance = instanceMetaData(i);
        instance.setSubnetId(subnetId);
        instance.setAvailabilityZone(availabilityZone);
        instance.setInstanceStatus(instanceStatus);
        return instance;
    }

    private InstanceMetaData instanceMetaData(int i) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setId((long) i);
        return instanceMetaData;
    }

    private Map<String, String> subnetAzPairs(int subnetCount) {
        Map<String, String> subnetAzPairs = new HashMap<>();
        for (int i = 0; i < subnetCount; i++) {
            subnetAzPairs.put(cloudSubnetName(i), cloudSubnetAz(i));
        }
        return subnetAzPairs;
    }

    private CloudSubnet cloudSubnet(int i) {
        CloudSubnet cloudSubnet = new CloudSubnet();
        cloudSubnet.setId(String.valueOf(i));
        cloudSubnet.setName(cloudSubnetName(i));
        cloudSubnet.setAvailabilityZone(cloudSubnetAz(i));
        return cloudSubnet;
    }

    private String cloudSubnetName(int i) {
        return "subnet-" + i;
    }

    private String cloudSubnetAz(int i) {
        return "az-" + i;
    }

    private void initSubnetIdAndAvailabilityZoneForInstances(List<Integer> existingCounts, InstanceGroupDto instanceGroup) {
        int totalExistingCount = existingCounts.stream()
                .mapToInt(Integer::intValue)
                .sum();
        Queue<Integer> existingSubnetIdIndexes = new ArrayDeque<>(totalExistingCount);
        int existingSubnetIdIndex = 0;
        for (int count : existingCounts) {
            for (int i = 0; i < count; i++) {
                existingSubnetIdIndexes.add(existingSubnetIdIndex);
            }
            existingSubnetIdIndex++;
        }
        instanceGroup.getInstanceMetadataViews().forEach(instance -> {
            Integer subnetIdIndex = existingSubnetIdIndexes.poll();
            if (subnetIdIndex != null) {
                ((InstanceMetaData) instance).setSubnetId(cloudSubnetName(subnetIdIndex));
                ((InstanceMetaData) instance).setAvailabilityZone(cloudSubnetAz(subnetIdIndex));
            }
        });
    }

    private void verifyCloudInstance(Set<Integer> expectedPermissibleSubnetIdIndexes, InstanceMetaData instanceMetaData) {
        Set<String> expectedPermissibleSubnetIds = null;
        Set<String> expectedPermissibleAvailabilityZones = null;
        if (!CollectionUtils.isEmpty(expectedPermissibleSubnetIdIndexes)) {
            expectedPermissibleSubnetIds = expectedPermissibleSubnetIdIndexes.stream()
                    .map(this::cloudSubnetName)
                    .collect(Collectors.toSet());
            expectedPermissibleAvailabilityZones = expectedPermissibleSubnetIdIndexes.stream()
                    .map(this::cloudSubnetAz)
                    .collect(Collectors.toSet());
        }

        verifyCloudInstance(expectedPermissibleSubnetIds, expectedPermissibleAvailabilityZones, instanceMetaData);
    }

    private void verifyCloudInstance(Set<String> expectedPermissibleSubnetIds, Set<String> expectedPermissibleAvailabilityZones,
            InstanceMetaData instanceMetaData) {
        if (CollectionUtils.isEmpty(expectedPermissibleSubnetIds)) {
            assertThat(instanceMetaData.getSubnetId()).isNull();
        } else {
            assertThat(instanceMetaData.getSubnetId()).isIn(expectedPermissibleSubnetIds);
        }

        if (CollectionUtils.isEmpty(expectedPermissibleAvailabilityZones)) {
            assertThat(instanceMetaData.getAvailabilityZone()).isNull();
        } else {
            assertThat(instanceMetaData.getAvailabilityZone()).isIn(expectedPermissibleAvailabilityZones);
        }
    }

    private void verifySubnetIdAndAvailabilityZoneForInstancesAreUnchanged(List<Integer> existingCounts, InstanceGroupDto instanceGroup,
            Set<InstanceMetaData> instancesToIgnore) {
        int countNum = existingCounts.size();
        Map<String, Integer> subnetIdToIndexMap = new HashMap<>(countNum);
        Map<String, Integer> availabilityZoneToIndexMap = new HashMap<>(countNum);
        for (int i = 0; i < countNum; i++) {
            subnetIdToIndexMap.put(cloudSubnetName(i), i);
            availabilityZoneToIndexMap.put(cloudSubnetAz(i), i);
        }

        Integer[] actualCountsArray = new Integer[countNum];
        Arrays.fill(actualCountsArray, 0);
        instanceGroup.getInstanceMetadataViews().forEach(instance -> {
            if (!instancesToIgnore.contains(instance)) {
                Integer subnetIdIndex = subnetIdToIndexMap.get(instance.getSubnetId());
                Integer availabilityZoneIndex = availabilityZoneToIndexMap.get(instance.getAvailabilityZone());
                assertThat(subnetIdIndex).isNotNull();
                assertThat(subnetIdIndex).isEqualTo(availabilityZoneIndex);
                actualCountsArray[subnetIdIndex]++;
            }
        });
        assertThat(Arrays.asList(actualCountsArray)).isEqualTo(existingCounts);
    }

    private InstanceGroup getARequestGroup(String hostGroup, int numOfNodes, InstanceGroupType hostGroupType) {
        InstanceGroup requestHostGroup = new InstanceGroup();
        requestHostGroup.setGroupName(hostGroup);
        requestHostGroup.setInstanceGroupType(hostGroupType);
        requestHostGroup.setInstanceGroupNetwork(new InstanceGroupNetwork());
        Set<InstanceMetaData> instanceMetadata = new HashSet<>();
        IntStream.range(0, numOfNodes).forEach(count -> instanceMetadata.add(new InstanceMetaData()));
        if ("gateway".equals(hostGroup) || "auxiliary".equals(hostGroup)) {
            instanceMetadata.forEach(metadata -> metadata.setId(1L));
        }
        instanceMetadata.stream()
                .forEach(metadata -> {
                    metadata.setInstanceStatus(SERVICES_RUNNING);
                });
        requestHostGroup.setInstanceMetaData(instanceMetadata);
        return requestHostGroup;
    }
}