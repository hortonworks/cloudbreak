package com.sequenceiq.cloudbreak.service.multiaz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
public class MultiAzCalculatorServiceTest {

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String AVAILABILITY_ZONE = "AVAILABILITY_ZONE";

    private static final int SINGLE_INSTANCE = 1;

    private static final int NO_SUBNETS = 0;

    private static final int SINGLE_SUBNET = 1;

    private static final String INSTANCE_ID = "i-123";

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
                { CloudPlatform.AWS,    1,  99,  Arrays.asList(99L),                  true   },
                { CloudPlatform.AWS,    3,  99,  Arrays.asList(33L, 33L, 33L),        true   },
                { CloudPlatform.AWS,    3,  60,  Arrays.asList(20L, 20L, 20L),        true   },
                { CloudPlatform.AWS,    3,  59,  Arrays.asList(20L, 20L, 19L),        true   },
                { CloudPlatform.AWS,    3,  58,  Arrays.asList(20L, 19L, 19L),        true   },
                { CloudPlatform.AWS,    3,  57,  Arrays.asList(19L, 19L, 19L),        true   },
                { CloudPlatform.AWS,    3,  56,  Arrays.asList(19L, 19L, 18L),        true   },
                { CloudPlatform.AWS,    4,  60,  Arrays.asList(15L, 15L, 15L, 15L),   true   },
                { CloudPlatform.AWS,    4,  59,  Arrays.asList(15L, 15L, 15L, 14L),   true   },
                { CloudPlatform.AWS,    4,  58,  Arrays.asList(15L, 15L, 14L, 14L),   true   },
                { CloudPlatform.AWS,    4,  57,  Arrays.asList(15L, 14L, 14L, 14L),   true   },
                { CloudPlatform.AWS,    4,  56,  Arrays.asList(14L, 14L, 14L, 14L),   true   },
                { CloudPlatform.YARN,   0,  0,   Arrays.asList(),                     false  },
        };
    }

    @ParameterizedTest(name = "testSubnetDistributionForWholeInstanceGroup " +
            "with {0} platform when {1} subnets and {2} instances should result in {3} subnet counts")
    @MethodSource("testSubnetDistributionForWholeInstanceGroupData")
    public void testSubnetDistributionForWholeInstanceGroup(CloudPlatform cloudPlatform, int subnetCount, int instanceCount, List<Long> expectedCounts,
            boolean supported) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        }
        InstanceGroup instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup);

        List<Long> actualCounts = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            int finalI = i;
            actualCounts.add(instanceGroup.getAllInstanceMetaData()
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
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        }

        InstanceGroup instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        InstanceMetaData instanceWithPrepopulatedForeignSubnetId = instanceMetaData(instanceCount, SUBNET_ID, AVAILABILITY_ZONE, InstanceStatus.REQUESTED);
        instanceGroup.getAllInstanceMetaData().add(instanceWithPrepopulatedForeignSubnetId);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup);

        List<Long> actualCounts = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            int finalI = i;
            actualCounts.add(instanceGroup.getAllInstanceMetaData()
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
                { CloudPlatform.AWS,    1,  99,  Arrays.asList(104L),                 true   },
                { CloudPlatform.AWS,    3,  99,  Arrays.asList(35L, 35L, 34L),        true   },
                { CloudPlatform.AWS,    3,  60,  Arrays.asList(22L, 22L, 21L),        true   },
                { CloudPlatform.AWS,    3,  59,  Arrays.asList(22L, 21L, 21L),        true   },
                { CloudPlatform.AWS,    3,  58,  Arrays.asList(21L, 21L, 21L),        true   },
                { CloudPlatform.AWS,    3,  57,  Arrays.asList(21L, 21L, 20L),        true   },
                { CloudPlatform.AWS,    3,  56,  Arrays.asList(21L, 20L, 20L),        true   },
                { CloudPlatform.AWS,    4,  60,  Arrays.asList(17L, 16L, 16L, 16L),   true   },
                { CloudPlatform.AWS,    4,  59,  Arrays.asList(16L, 16L, 16L, 16L),   true   },
                { CloudPlatform.AWS,    4,  58,  Arrays.asList(16L, 16L, 16L, 15L),   true   },
                { CloudPlatform.AWS,    4,  57,  Arrays.asList(16L, 16L, 15L, 15L),   true   },
                { CloudPlatform.AWS,    4,  56,  Arrays.asList(16L, 15L, 15L, 15L),   true   },
                { CloudPlatform.YARN,   0,  0,   Arrays.asList(),                     false  },
        };
    }

    @ParameterizedTest(name = "testSubnetDistributionForWholeInstanceGroupWhen5InstancesWithPrepopulatedKnownSubnetId " +
            "with {0} platform when {1} subnets and {2} instances should result in {3} subnet counts")
    @MethodSource("testSubnetDistributionForWholeInstanceGroupWhen5InstancesWithPrepopulatedKnownSubnetIdData")
    public void testSubnetDistributionForWholeInstanceGroupWhen5InstancesWithPrepopulatedKnownSubnetId(CloudPlatform cloudPlatform, int subnetCount,
            int instanceCount, List<Long> expectedCounts, boolean supported) {
        if (supported) {
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        }

        InstanceGroup instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        Set<InstanceMetaData> extraInstancesWithPrepopulatedSubnetId = new HashSet<>();
        String subnetIdForExtraInstances = cloudSubnetName(0);
        String availabilityZoneForExtraInstances = cloudSubnetAz(0);
        if (subnetCount > 0) {
            for (int i = 0; i < 5; i++) {
                extraInstancesWithPrepopulatedSubnetId.add(instanceMetaData(instanceCount + i, subnetIdForExtraInstances, availabilityZoneForExtraInstances,
                        null));
            }
        }
        instanceGroup.getAllInstanceMetaData().addAll(extraInstancesWithPrepopulatedSubnetId);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup);

        List<Long> actualCounts = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            int finalI = i;
            actualCounts.add(instanceGroup.getAllInstanceMetaData()
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
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        }

        InstanceGroup instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
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
        instanceGroup.getAllInstanceMetaData().addAll(deletedInstancesWithPrepopulatedSubnetId);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup);

        List<Long> actualCounts = new ArrayList<>();
        for (int i = 0; i < subnetCount; i++) {
            int finalI = i;
            actualCounts.add(instanceGroup.getAllInstanceMetaData()
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
        InstanceGroup instanceGroup = instanceGroup(CloudPlatform.AWS, SINGLE_INSTANCE, NO_SUBNETS);
        if (disableNetwork) {
            instanceGroup.setInstanceGroupNetwork(null);
        } else if (disableAttributes) {
            instanceGroup.getInstanceGroupNetwork().setAttributes(null);
        } else if (disableSubnetIdsAttribute) {
            instanceGroup.getInstanceGroupNetwork().setAttributes(new Json(Map.of()));
        }

        underTest.calculateByRoundRobin(
                subnetAzPairs(NO_SUBNETS),
                instanceGroup);

        instanceGroup.getAllInstanceMetaData().forEach(instance -> {
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

        InstanceGroup instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        initSubnetIdAndAvailabilityZoneForInstances(existingCounts, instanceGroup);

        CloudInstance cloudInstance = new CloudInstance(INSTANCE_ID, null, null, null, null);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup,
                cloudInstance);

        verifyCloudInstance(expectedPermissibleSubnetIdIndexes, cloudInstance);
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

        InstanceGroup instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
        initSubnetIdAndAvailabilityZoneForInstances(existingCounts, instanceGroup);
        InstanceMetaData instanceWithPrepopulatedForeignSubnetId = instanceMetaData(instanceCount, SUBNET_ID, AVAILABILITY_ZONE, InstanceStatus.REQUESTED);
        instanceGroup.getAllInstanceMetaData().add(instanceWithPrepopulatedForeignSubnetId);

        CloudInstance cloudInstance = new CloudInstance(INSTANCE_ID, null, null, null, null);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup,
                cloudInstance);

        verifyCloudInstance(expectedPermissibleSubnetIdIndexes, cloudInstance);
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
            when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);
        }

        InstanceGroup instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);
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
                    InstanceStatus.TERMINATED));
            deletedInstancesWithPrepopulatedSubnetId.add(instanceMetaData(instanceCount + 2, subnetIdForDeletedInstances, availabilityZoneForDeletedInstances,
                    InstanceStatus.DELETED_BY_PROVIDER));
            deletedInstancesWithPrepopulatedSubnetId.add(instanceMetaData(instanceCount + 3, subnetIdForDeletedInstances, availabilityZoneForDeletedInstances,
                    InstanceStatus.DELETED_ON_PROVIDER_SIDE));
        }
        instanceGroup.getAllInstanceMetaData().addAll(deletedInstancesWithPrepopulatedSubnetId);

        CloudInstance cloudInstance = new CloudInstance(INSTANCE_ID, null, null, null, null);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup,
                cloudInstance);

        verifyCloudInstance(expectedPermissibleSubnetIdIndexes, cloudInstance);
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
        InstanceGroup instanceGroup = instanceGroup(CloudPlatform.AWS, SINGLE_INSTANCE, NO_SUBNETS);
        if (disableNetwork) {
            instanceGroup.setInstanceGroupNetwork(null);
        } else if (disableAttributes) {
            instanceGroup.getInstanceGroupNetwork().setAttributes(null);
        } else if (disableSubnetIdsAttribute) {
            instanceGroup.getInstanceGroupNetwork().setAttributes(new Json(Map.of()));
        }

        String cloudInstanceSubnetId = prepopulateCloudInstanceSubnet ? SUBNET_ID : null;
        String cloudInstanceAvailabilityZone = prepopulateCloudInstanceSubnet ? AVAILABILITY_ZONE : null;
        CloudInstance cloudInstance = new CloudInstance(INSTANCE_ID, null, null, cloudInstanceSubnetId, cloudInstanceAvailabilityZone);

        underTest.calculateByRoundRobin(
                subnetAzPairs(NO_SUBNETS),
                instanceGroup,
                cloudInstance);

        verifyCloudInstance(cloudInstanceSubnetId == null ? Set.of() : Set.of(cloudInstanceSubnetId),
                cloudInstanceAvailabilityZone == null ? Set.of() : Set.of(cloudInstanceAvailabilityZone),
                cloudInstance);

        instanceGroup.getAllInstanceMetaData().forEach(instance -> {
            assertThat(instance.getSubnetId()).isNull();
            assertThat(instance.getAvailabilityZone()).isNull();
        });
    }

    @Test
    public void calculateByRoundRobinTestWhenCloudInstanceAndPrepopulatedCloudInstanceSubnet() {
        when(multiAzValidator.supportedForInstanceMetadataGeneration(any(InstanceGroup.class))).thenReturn(true);

        List<Integer> existingCounts = List.of(SINGLE_SUBNET);
        InstanceGroup instanceGroup = instanceGroup(CloudPlatform.AWS, SINGLE_INSTANCE, SINGLE_SUBNET);
        initSubnetIdAndAvailabilityZoneForInstances(existingCounts, instanceGroup);

        CloudInstance cloudInstance = new CloudInstance(INSTANCE_ID, null, null, SUBNET_ID, AVAILABILITY_ZONE);

        underTest.calculateByRoundRobin(
                subnetAzPairs(SINGLE_SUBNET),
                instanceGroup,
                cloudInstance);

        verifyCloudInstance(Set.of(SUBNET_ID), Set.of(AVAILABILITY_ZONE), cloudInstance);
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

        InstanceGroup instanceGroup = instanceGroup(cloudPlatform, instanceCount, subnetCount);

        CloudInstance cloudInstance = new CloudInstance(INSTANCE_ID, null, null, null, null);

        underTest.calculateByRoundRobin(
                subnetAzPairs(subnetCount),
                instanceGroup,
                cloudInstance);

        verifyCloudInstance(expectedPermissibleSubnetIdIndexes, cloudInstance);

        instanceGroup.getAllInstanceMetaData().forEach(instance -> {
            assertThat(instance.getSubnetId()).isNull();
            assertThat(instance.getAvailabilityZone()).isNull();
        });
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

    private InstanceGroup instanceGroup(CloudPlatform cloudPlatform, int instanceNumber, int subnetCount) {
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
        return instanceGroup;
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
        return "name-" + i;
    }

    private String cloudSubnetAz(int i) {
        return "az-" + i;
    }

    private void initSubnetIdAndAvailabilityZoneForInstances(List<Integer> existingCounts, InstanceGroup instanceGroup) {
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
        instanceGroup.getAllInstanceMetaData().forEach(instance -> {
            Integer subnetIdIndex = existingSubnetIdIndexes.poll();
            if (subnetIdIndex != null) {
                instance.setSubnetId(cloudSubnetName(subnetIdIndex));
                instance.setAvailabilityZone(cloudSubnetAz(subnetIdIndex));
            }
        });
    }

    private void verifyCloudInstance(Set<Integer> expectedPermissibleSubnetIdIndexes, CloudInstance cloudInstance) {
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

        verifyCloudInstance(expectedPermissibleSubnetIds, expectedPermissibleAvailabilityZones, cloudInstance);
    }

    private void verifyCloudInstance(Set<String> expectedPermissibleSubnetIds, Set<String> expectedPermissibleAvailabilityZones, CloudInstance cloudInstance) {
        if (CollectionUtils.isEmpty(expectedPermissibleSubnetIds)) {
            assertThat(cloudInstance.getSubnetId()).isNull();
        } else {
            assertThat(cloudInstance.getSubnetId()).isIn(expectedPermissibleSubnetIds);
        }

        if (CollectionUtils.isEmpty(expectedPermissibleAvailabilityZones)) {
            assertThat(cloudInstance.getAvailabilityZone()).isNull();
        } else {
            assertThat(cloudInstance.getAvailabilityZone()).isIn(expectedPermissibleAvailabilityZones);
        }

        assertThat(cloudInstance.getInstanceId()).isEqualTo(INSTANCE_ID);
        assertThat(cloudInstance.getAuthentication()).isNull();
        assertThat(cloudInstance.getTemplate()).isNull();
        assertThat(cloudInstance.getParameters()).isNotNull();
        assertThat(cloudInstance.getParameters()).isEmpty();
    }

    private void verifySubnetIdAndAvailabilityZoneForInstancesAreUnchanged(List<Integer> existingCounts, InstanceGroup instanceGroup,
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
        instanceGroup.getAllInstanceMetaData().forEach(instance -> {
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

}