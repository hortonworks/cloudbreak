package com.sequenceiq.freeipa.service.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityInfo;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

public class FreeipaDownscaleNodeCalculatorServiceTest {

    private final FreeipaDownscaleNodeCalculatorService underTest = new FreeipaDownscaleNodeCalculatorService();

    @Test
    void testCalculateDownscaleCandidatesWhenInstanceIdsToDeleteIsNull() {
        Stack stack = mock(Stack.class);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(getNotDeletedInstanceMetadataSet(3));
        AvailabilityInfo availabilityInfo = new AvailabilityInfo(3);

        ArrayList<String> downscaleCandidates = underTest.calculateDownscaleCandidates(stack, availabilityInfo, AvailabilityType.TWO_NODE_BASED, null);

        assertThat(downscaleCandidates).asList()
                .hasSize(1);
    }

    @Test
    void testCalculateDownscaleCandidatesWhenInstanceIdsToDeleteIsEmpty() {
        Stack stack = mock(Stack.class);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(getNotDeletedInstanceMetadataSet(3));
        AvailabilityInfo availabilityInfo = new AvailabilityInfo(3);

        ArrayList<String> downscaleCandidates = underTest.calculateDownscaleCandidates(stack, availabilityInfo, AvailabilityType.TWO_NODE_BASED, Set.of());

        assertThat(downscaleCandidates).asList()
                .hasSize(1);
    }

    static Object [] [] dataForMultiAzDownScale() {
        return new Object [] [] {
                { Map.of(), null},
                { Map.of("1", List.of("instance0", "instance1"), "2", List.of("instance2")), "1"},
                { Map.of("1", List.of("instance0"), "2", List.of("instance1", "instance2")), "2"},
                { Map.of("1", List.of("instance0"), "2", List.of("instance1"), "3", List.of("instance2", "instance3")), "3"}
        };
    }

    @ParameterizedTest
    @MethodSource("dataForMultiAzDownScale")
    void testCalculateDownscaleCandidatesWhenInstanceIdsToDeleteIsEmptyForMultiAz(Map<String, List<String>> instanceData, String expectedZone) {
        Stack stack = mock(Stack.class);
        when(stack.isMultiAz()).thenReturn(true);

        Set<InstanceMetaData> instances = new HashSet<>();
        instanceData.entrySet().stream().forEach(entry -> {
            entry.getValue().stream().forEach(value -> {
                InstanceMetaData instance = createInstanceMetadata(value, entry.getKey());
                instances.add(instance);
            });
        });

        Map<String, List<String>> availabilityZoneToNodesMap = instances.stream().collect(Collectors.toMap(instance -> instance.getAvailabilityZone(),
                instance -> Stream.of(instance.getInstanceId()).collect(Collectors.toList()), (first, second) -> {
                    first.addAll(second);
                    return first;
                }));

        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instances);
        AvailabilityInfo availabilityInfo = new AvailabilityInfo(3);

        ArrayList<String> downscaleCandidates = underTest.calculateDownscaleCandidates(stack, availabilityInfo, AvailabilityType.TWO_NODE_BASED, Set.of());

        assertThat(downscaleCandidates).asList()
                .hasSize(expectedZone != null ? 1 : 0);

        if (expectedZone != null) {
            assertEquals(true, availabilityZoneToNodesMap.get(expectedZone).contains(downscaleCandidates.get(0)));
        }

    }

    @Test
    void testCalculateDownscaleCandidatesWhenInstanceIdIsProvided() {
        Stack stack = mock(Stack.class);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(getNotDeletedInstanceMetadataSet(3));
        AvailabilityInfo availabilityInfo = new AvailabilityInfo(3);
        Set<String> instanceIdsToDelete = Set.of("im_0");

        ArrayList<String> downscaleCandidates = underTest.calculateDownscaleCandidates(stack, availabilityInfo, null, instanceIdsToDelete);

        assertThat(downscaleCandidates).asList()
                .hasSize(1)
                .hasSameElementsAs(Set.of("im_0"));
    }

    @Test
    void testCalculateTargetAvailabilityTypeWhenAvailabilityTypeSpecified() {
        DownscaleRequest downscaleRequest = new DownscaleRequest();
        downscaleRequest.setTargetAvailabilityType(AvailabilityType.TWO_NODE_BASED);

        AvailabilityType targetAvailabilityType = underTest.calculateTargetAvailabilityType(downscaleRequest, 3);

        assertEquals(AvailabilityType.TWO_NODE_BASED, targetAvailabilityType);
    }

    @ParameterizedTest
    @MethodSource(value = "createDeleteNodeCountToAvailabilityTypeMapping")
    void testCalculateTargetAvailabilityTypeWhenInstanceIdsSpecified(int instanceIdCount, AvailabilityType expectedTargetAvailabilityType) {
        DownscaleRequest downscaleRequest = new DownscaleRequest();
        downscaleRequest.setInstanceIds(getInstanceIds(instanceIdCount));

        AvailabilityType targetAvailabilityType = underTest.calculateTargetAvailabilityType(downscaleRequest, 3);

        assertEquals(expectedTargetAvailabilityType, targetAvailabilityType);
    }

    static Object [][] createDeleteNodeCountToAvailabilityTypeMapping() {
        return new Object[][] {
                {1, AvailabilityType.TWO_NODE_BASED},
                {2, AvailabilityType.NON_HA}
        };
    }

    private Set<String> getInstanceIds(int instanceIdCount) {
        return IntStream.range(0, instanceIdCount)
                .mapToObj(this::generateInstanceMetadataName)
                .collect(Collectors.toSet());
    }

    private Set<InstanceMetaData> getNotDeletedInstanceMetadataSet(int nodeCount) {
        return getInstanceIds(nodeCount).stream()
                .map(this::createInstanceMetadata)
                .collect(Collectors.toSet());
    }

    private InstanceMetaData createInstanceMetadata(String id) {
        return createInstanceMetadata(id, null);
    }

    private InstanceMetaData createInstanceMetadata(String id, String availabilityZone) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(id);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        instanceMetaData.setAvailabilityZone(availabilityZone);
        return instanceMetaData;
    }

    private String generateInstanceMetadataName(int id) {
        return "im_" + id;
    }

}