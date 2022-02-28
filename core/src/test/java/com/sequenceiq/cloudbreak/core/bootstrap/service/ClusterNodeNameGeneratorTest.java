package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@RunWith(MockitoJUnitRunner.class)
public class ClusterNodeNameGeneratorTest {

    @Mock
    private HostDiscoveryService hostDiscoveryService;

    @InjectMocks
    private ClusterNodeNameGenerator underTest;

    @Before
    public void setUp() {
        when(hostDiscoveryService.calculateHostname(anyString(), nullable(String.class),
                nullable(String.class), anyLong(), anyBoolean())).thenCallRealMethod();
        when(hostDiscoveryService.generateHostname(anyString(), anyString(),
                anyLong(), anyBoolean())).thenCallRealMethod();
    }

    @Test
    public void testGetNodeNameForInstanceWhenNewCluster() {
        Stack nodeNameStack = new Stack();
        nodeNameStack.setHostgroupNameAsHostname(true);
        nodeNameStack.setCustomHostname("teststack");

        InstanceGroup master = new InstanceGroup();
        master.setGroupName("master");
        initializeHostGroupInstanceMetadata(1, master, List.of(""));

        InstanceGroup worker = new InstanceGroup();
        worker.setGroupName("worker");
        initializeHostGroupInstanceMetadata(3, worker, List.of("", "", ""));

        InstanceGroup compute = new InstanceGroup();
        compute.setGroupName("compute");
        initializeHostGroupInstanceMetadata(3, compute, List.of("", "", ""));
        nodeNameStack.setInstanceGroups(Set.of(master, compute, worker));

        Set<String> expectedNodeNames = Set.of("teststack-master0",
                "teststack-compute0", "teststack-compute1", "teststack-compute2",
                "teststack-worker0", "teststack-worker2", "teststack-worker1");
        testHostNameGenerationForStack(nodeNameStack, expectedNodeNames);
    }

    @Test
    public void testGetNodeNameForInstanceWhenScalingExistingPrivateIdBasedCluster() {
        Stack nodeNameStack = new Stack();
        nodeNameStack.setHostgroupNameAsHostname(true);
        nodeNameStack.setCustomHostname("teststack");

        InstanceGroup compute = new InstanceGroup();
        compute.setGroupName("compute");

        initializeHostGroupInstanceMetadata(6, compute, List.of("teststack-compute12.testdomain",
                "teststack-compute13.testdomain", "teststack-compute14.testdomain", "", "", ""));
        nodeNameStack.setInstanceGroups(Set.of(compute));

        Set<String> expectedNodeNames = Set.of(
                "teststack-compute12", "teststack-compute13", "teststack-compute14", "teststack-compute0",
                "teststack-compute1", "teststack-compute2");
        testHostNameGenerationForStack(nodeNameStack, expectedNodeNames);
    }

    @Test
    public void testGetNodeNameForInstanceWhenScalingFromZero() {
        Stack nodeNameStack = new Stack();
        nodeNameStack.setHostgroupNameAsHostname(true);
        nodeNameStack.setCustomHostname("teststack");

        InstanceGroup compute = new InstanceGroup();
        compute.setGroupName("compute");

        initializeHostGroupInstanceMetadata(6, compute, List.of("", "", "", "", "", ""));
        nodeNameStack.setInstanceGroups(Set.of(compute));

        Set<String> expectedNodeNames = Set.of(
                "teststack-compute0", "teststack-compute1", "teststack-compute2", "teststack-compute3",
                "teststack-compute4", "teststack-compute5");
        testHostNameGenerationForStack(nodeNameStack, expectedNodeNames);
    }

    @Test
    public void testGetNodeNameForInstanceWhenScaling() {
        Stack nodeNameStack = new Stack();
        nodeNameStack.setHostgroupNameAsHostname(true);
        nodeNameStack.setCustomHostname("teststack");

        InstanceGroup compute = new InstanceGroup();
        compute.setGroupName("compute");
        //Scaling 3 node "compute" to 5 node "compute".
        initializeHostGroupInstanceMetadata(5, compute, List.of("teststack-compute0.testdomain",
                "teststack-compute1.testdomain", "teststack-compute2.testdomain", "", ""));
        nodeNameStack.setInstanceGroups(Set.of(compute));

        Set<String> expectedNodeNames = Set.of("teststack-compute0", "teststack-compute1",
                "teststack-compute2", "teststack-compute3", "teststack-compute4");
        testHostNameGenerationForStack(nodeNameStack, expectedNodeNames);
    }

    @Test
    public void testGetNodeNameForInstanceWhenResizingAndWorker() {
        Stack nodeNameStack = new Stack();
        nodeNameStack.setHostgroupNameAsHostname(true);
        nodeNameStack.setCustomHostname("teststack");

        InstanceGroup worker = new InstanceGroup();
        worker.setGroupName("worker");
        initializeHostGroupInstanceMetadata(5, worker, List.of("teststack-worker3.testdomain",
                "teststack-worker4.testdomain", "teststack-worker5.testdomain", "", ""));
        nodeNameStack.setInstanceGroups(Set.of(worker));

        Set<String> expectedNodeNames = Set.of("teststack-worker3", "teststack-worker4",
                "teststack-worker5", "teststack-worker0", "teststack-worker1");
        testHostNameGenerationForStack(nodeNameStack, expectedNodeNames);
    }

    private void testHostNameGenerationForStack(Stack underTestStack, Set<String> expectedNodeNames) {
        Set<InstanceMetaData> testInstanceMetadata = underTestStack.getNotTerminatedInstanceMetaDataSet();
        Map<String, AtomicLong> hostGroupNodeCount = new HashMap<>();
        underTestStack.getInstanceGroups().stream().forEach(
                instanceGroup -> hostGroupNodeCount.put(instanceGroup.getGroupName(), new AtomicLong(0L)));

        Set<String> clusterNodeNames = underTestStack.getInstanceMetaDataAsList().stream()
                .map(InstanceMetaData::getShortHostname).collect(Collectors.toSet());

        Set<String> resultSet = new HashSet();
        for (InstanceMetaData im : testInstanceMetadata) {
            String generatedHostName = underTest.getNodeNameForInstanceMetadata(im, underTestStack, hostGroupNodeCount, clusterNodeNames);
            resultSet.add(generatedHostName);
        }
        assertEquals("InstanceMetadata size should match", expectedNodeNames.size(), testInstanceMetadata.size());
        assertEquals("Generated Hostname should match", expectedNodeNames, resultSet);
    }

    private void initializeHostGroupInstanceMetadata(int nodeCount, InstanceGroup instanceGroup, List<String> fqdns) {
        IntStream.range(0, nodeCount).forEach(
                nodeIndex -> {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    instanceMetaData.setInstanceGroup(instanceGroup);
                    instanceGroup.getInstanceMetaDataSet().add(instanceMetaData);

                    if (!fqdns.get(nodeIndex).isBlank()) {
                        instanceMetaData.setDiscoveryFQDN(fqdns.get(nodeIndex));
                    }
                }
        );
    }
}