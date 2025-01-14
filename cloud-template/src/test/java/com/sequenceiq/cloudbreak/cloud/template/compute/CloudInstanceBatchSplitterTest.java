package com.sequenceiq.cloudbreak.cloud.template.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

@ExtendWith(MockitoExtension.class)
class CloudInstanceBatchSplitterTest {

    private CloudInstanceBatchSplitter underTest;

    @BeforeEach
    void setUp() {
        underTest = new CloudInstanceBatchSplitter();
    }

    @Test
    void splitGroupsWithNoInstances() {
        List<Group> groups = generateGroups(3, 0);
        List<CloudInstancesGroupProcessingBatch> batches = underTest.split(groups, 4);
        assertEquals(3, batches.size());
    }

    @Test
    void splitGroupsWithBatchSizeOne() {
        List<Group> groups = generateGroups(2, 3);
        List<CloudInstancesGroupProcessingBatch> batches = underTest.split(groups, 1);
        assertEquals(2, batches.size());
        assertEquals("group0", batches.get(0).getGroup().getName());
        assertEquals("group1", batches.get(1).getGroup().getName());
        assertEquals(3, batches.get(0).getCloudInstances().size());
        assertEquals(3, batches.get(1).getCloudInstances().size());
        assertEquals("group0;instance0", batches.get(0).getCloudInstances().get(0).get(0).getInstanceId());
        assertEquals("group0;instance1", batches.get(0).getCloudInstances().get(1).get(0).getInstanceId());
        assertEquals("group0;instance2", batches.get(0).getCloudInstances().get(2).get(0).getInstanceId());
        assertEquals("group1;instance0", batches.get(1).getCloudInstances().get(0).get(0).getInstanceId());
        assertEquals("group1;instance1", batches.get(1).getCloudInstances().get(1).get(0).getInstanceId());
        assertEquals("group1;instance2", batches.get(1).getCloudInstances().get(2).get(0).getInstanceId());
    }

    @Test
    void splitGroupsWithBatchSizeFive() {
        List<Group> groups = generateGroups(2, 3);
        List<CloudInstancesGroupProcessingBatch> batches = underTest.split(groups, 5);
        assertEquals(2, batches.size());
        assertEquals("group0", batches.get(0).getGroup().getName());
        assertEquals("group1", batches.get(1).getGroup().getName());
        assertEquals(1, batches.get(0).getCloudInstances().size());
        assertEquals(1, batches.get(1).getCloudInstances().size());
        assertEquals("group0;instance0", batches.get(0).getCloudInstances().get(0).get(0).getInstanceId());
        assertEquals("group0;instance1", batches.get(0).getCloudInstances().get(0).get(1).getInstanceId());
        assertEquals("group0;instance2", batches.get(0).getCloudInstances().get(0).get(2).getInstanceId());
        assertEquals("group1;instance0", batches.get(1).getCloudInstances().get(0).get(0).getInstanceId());
        assertEquals("group1;instance1", batches.get(1).getCloudInstances().get(0).get(1).getInstanceId());
        assertEquals("group1;instance2", batches.get(1).getCloudInstances().get(0).get(2).getInstanceId());
    }

    @Test
    void splitGroupsWithBatchSizeFiveManyInstances() {
        List<Group> groups = generateGroups(2, 6);
        List<CloudInstancesGroupProcessingBatch> batches = underTest.split(groups, 5);
        assertEquals(2, batches.size());
        assertEquals("group0", batches.get(0).getGroup().getName());
        assertEquals("group1", batches.get(1).getGroup().getName());
        assertEquals(2, batches.get(0).getCloudInstances().size());
        assertEquals(2, batches.get(1).getCloudInstances().size());
    }

    @Test
    void splitGroupsWithBatchSizeZeroThrowsException() {
        List<Group> groups = generateGroups(2, 3);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.split(groups, 0);
        });
        assertEquals("Batch size must be greater than 0", exception.getMessage());
    }

    @Test
    void splitGroupsWithNullGroupsThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.split(null, 4);
        });
        assertEquals("Instance groups cannot be null", exception.getMessage());
    }

    @Test
    void splitGroupsWithNullInstancesThrowsException() {
        List<Group> groups = new ArrayList<>();
        Group group = mock(Group.class);
        when(group.getInstances()).thenReturn(null);
        groups.add(group);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.split(groups, 4);
        });
        assertEquals("Instances in group cannot be null", exception.getMessage());
    }

    private List<Group> generateGroups(int numberOfGroups, int numberofInstances) {
        List<Group> groups = new ArrayList<>();
        for (int i = 0; i < numberOfGroups; i++) {
            Group group = mock(Group.class);
            lenient().when(group.getName()).thenReturn("group" + i);
            InstanceTemplate template = mock(InstanceTemplate.class);
            lenient().when(template.getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
            List<CloudInstance> instances = new ArrayList<>();
            for (int j = 0; j < numberofInstances; j++) {
                CloudInstance cloudInstance = mock(CloudInstance.class);
                lenient().when(cloudInstance.getTemplate()).thenReturn(template);
                lenient().when(cloudInstance.getInstanceId()).thenReturn("group" + i + ";instance" + j);
                instances.add(cloudInstance);
            }
            lenient().when(group.getInstances()).thenReturn(instances);
            groups.add(group);
        }
        return groups;
    }
}