package com.sequenceiq.cloudbreak.ambari;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.filter.HostFilterService;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariClientPollerObject;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.cluster.service.NotRecommendedNodeRemovalException;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.filter.ConfigParam;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class AmbariDecommissionerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private final AmbariDecommissioner underTest = new AmbariDecommissioner();

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private AmbariConfigurationService configurationService;

    @Captor
    private ArgumentCaptor<Map<String, String>> stringStringCaptor;

    @Mock
    private HostFilterService hostFilterService;

    @Mock
    private AmbariDecommissionTimeCalculator ambariDecommissionTimeCalculator;

    @Mock
    private PollingService<AmbariClientPollerObject> ambariClientPollingService;

    @Test
    public void testSelectNodesWhenHasOneUnhealthyNodeAndShouldSelectOne() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";

        HostMetadata unhealhtyNode = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode = getHostMetadata(hostname2, HostMetadataState.HEALTHY);

        Collection<HostMetadata> nodes = Arrays.asList(unhealhtyNode, healhtyNode);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);
        ascendingNodes.put(hostname2, 110L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 1);

        assertEquals(1L, selectedNodes.size());
        assertEquals(hostname1, selectedNodes.keySet().stream().findFirst().get());
    }

    @Test
    public void testSelectNodesWhenHasOneUnhealthyNodeAndShouldSelectTwo() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";
        String hostname3 = "10.0.0.3";

        HostMetadata unhealhtyNode = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode1 = getHostMetadata(hostname2, HostMetadataState.HEALTHY);
        HostMetadata healhtyNode2 = getHostMetadata(hostname3, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Arrays.asList(unhealhtyNode, healhtyNode1, healhtyNode2);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);
        ascendingNodes.put(hostname2, 110L);
        ascendingNodes.put(hostname3, 120L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 2);

        assertEquals(2L, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().containsAll(Arrays.asList(hostname1, hostname2)));
    }

    @Test
    public void testSelectNodesWhenHasThreeUnhealthyNodeAndShouldSelectTwo() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";
        String hostname3 = "10.0.0.3";
        String hostname4 = "10.0.0.4";
        String hostname5 = "10.0.0.5";

        HostMetadata unhealhtyNode1 = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata unhealhtyNode2 = getHostMetadata(hostname2, HostMetadataState.UNHEALTHY);
        HostMetadata unhealhtyNode3 = getHostMetadata(hostname3, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode1 = getHostMetadata(hostname4, HostMetadataState.HEALTHY);
        HostMetadata healhtyNode2 = getHostMetadata(hostname5, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Arrays.asList(unhealhtyNode1, unhealhtyNode2, unhealhtyNode3, healhtyNode1, healhtyNode2);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);
        ascendingNodes.put(hostname2, 110L);
        ascendingNodes.put(hostname3, 120L);
        ascendingNodes.put(hostname4, 130L);
        ascendingNodes.put(hostname5, 140L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 2);

        assertEquals(2L, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().containsAll(Arrays.asList(hostname1, hostname2)));
    }

    @Test
    public void testSelectNodesWhenHasOneUnhealthyNodeButNotInAscendingList() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";
        String hostname3 = "10.0.0.3";

        HostMetadata unhealhtyNode1 = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode1 = getHostMetadata(hostname2, HostMetadataState.HEALTHY);
        HostMetadata healhtyNode2 = getHostMetadata(hostname3, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Arrays.asList(unhealhtyNode1, healhtyNode1, healhtyNode2);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname2, 110L);
        ascendingNodes.put(hostname3, 120L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 1);

        assertEquals(1L, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().contains(hostname2));
    }

    @Test
    public void testSelectNodesWhenHostNameShouldContainsInAscNodesAndNodes() {

        String hostname1 = "10.0.0.1";

        HostMetadata healhtyNode1 = getHostMetadata(hostname1, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Collections.singletonList(healhtyNode1);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 1);

        assertEquals(1L, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().contains(hostname1));
    }

    @Test
    public void testVerifyNodesAreRemovableWithReplicationFactor() {
        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";

        Blueprint blueprint = new Blueprint();
        blueprint.setName(ambariName);
        blueprint.setStackName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setBlueprint(blueprint);

        Stack stack = new Stack();
        stack.setCluster(cluster);
        stack.setGatewayPort(gatewayPort);
        stack.setPlatformVariant("GCP");
        stack.setId(100L);

        InstanceGroup masterInstanceGroup = getMasterInstanceGroup();
        InstanceGroup slaveInstanceGroup = getSlaveInstanceGroup(100);
        stack.setInstanceGroups(Sets.newHashSet(masterInstanceGroup, slaveInstanceGroup));

        HostGroup masterHostGroup = getHostGroupForInstanceGroup(masterInstanceGroup, 1L);
        HostGroup slaveHostGroup = getHostGroupForInstanceGroup(slaveInstanceGroup, 2L);
        cluster.setHostGroups(Sets.newHashSet(masterHostGroup, slaveHostGroup));

        Map<String, List<String>> blueprintMap = new HashMap<>();
        blueprintMap.put(slaveHostGroup.getName(), Collections.singletonList("DATANODE"));

        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);
        when(configurationService.getConfiguration(ambariClient, slaveHostGroup.getName()))
                .thenReturn(Collections.singletonMap(ConfigParam.DFS_REPLICATION.key(), "3"));
        when(ambariClientPollingService.pollWithTimeoutSingleFailure(any(), any(), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS);

        List<InstanceMetaData> removableNodes =
                slaveInstanceGroup.getAllInstanceMetaData().stream()
                        .filter(instanceMetaData -> instanceMetaData.getPrivateId() < 3L)
                        .collect(Collectors.toList());

        Set<String> removableHostnames = removableNodes.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toSet());
        doAnswer(invocation -> slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> removableHostnames.contains(hostMetadata.getHostName()))
                .collect(Collectors.toList())).when(hostFilterService).filterHostsForDecommission(any(), any(), any(), any(), any());
        Multimap<Long, HostMetadata> hostGroupWithInstances = ArrayListMultimap.create();
        slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> removableHostnames.contains(hostMetadata.getHostName()))
                .forEach(hostMetadata -> {
                    hostGroupWithInstances.put(slaveHostGroup.getId(), hostMetadata);
                });

        underTest.verifyNodesAreRemovable(stack, hostGroupWithInstances, Sets.newHashSet(masterHostGroup, slaveHostGroup), 50, ambariClient,
                removableNodes);

        verify(ambariDecommissionTimeCalculator).calculateDecommissioningTime(any(), any(), any(), anyLong(), anyInt());
    }

    @Test
    public void testVerifyNodesAreRemovableFilterOutNodes() {

        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";

        Blueprint blueprint = new Blueprint();
        blueprint.setName(ambariName);
        blueprint.setStackName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setBlueprint(blueprint);

        Stack stack = new Stack();
        stack.setCluster(cluster);
        stack.setGatewayPort(gatewayPort);
        stack.setId(100L);

        InstanceGroup masterInstanceGroup = getMasterInstanceGroup();
        InstanceGroup slaveInstanceGroup = getSlaveInstanceGroup(100);
        stack.setInstanceGroups(Sets.newHashSet(masterInstanceGroup, slaveInstanceGroup));

        HostGroup masterHostGroup = getHostGroupForInstanceGroup(masterInstanceGroup, 1L);
        HostGroup slaveHostGroup = getHostGroupForInstanceGroup(slaveInstanceGroup, 2L);
        cluster.setHostGroups(Sets.newHashSet(masterHostGroup, slaveHostGroup));

        Map<String, List<String>> blueprintMap = new HashMap<>();
        blueprintMap.put(slaveHostGroup.getName(), Collections.singletonList("DATANODE"));

        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);
        when(configurationService.getConfiguration(ambariClient, slaveHostGroup.getName()))
                .thenReturn(Collections.singletonMap(ConfigParam.DFS_REPLICATION.key(), "3"));

        List<InstanceMetaData> removableNodes =
                slaveInstanceGroup.getAllInstanceMetaData().stream()
                        .filter(instanceMetaData -> instanceMetaData.getPrivateId() < 3L)
                        .collect(Collectors.toList());

        Set<String> removableHostnames = removableNodes.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toSet());
        doAnswer(invocation -> slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> !"10-0-1-0.example.com".equals(hostMetadata.getHostName()))
                .filter(hostMetadata -> removableHostnames.contains(hostMetadata.getHostName()))
                .collect(Collectors.toList())).when(hostFilterService).filterHostsForDecommission(any(), any(), any(), any(), any());

        Multimap<Long, HostMetadata> hostGroupWithInstances = ArrayListMultimap.create();
        slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> removableHostnames.contains(hostMetadata.getHostName()))
                .forEach(hostMetadata -> {
            hostGroupWithInstances.put(slaveHostGroup.getId(), hostMetadata);
        });
        thrown.expect(NotRecommendedNodeRemovalException.class);
        thrown.expectMessage("Following nodes shouldn't be removed from the cluster: [10-0-1-0.example.com]");

        underTest.verifyNodesAreRemovable(stack, hostGroupWithInstances, Sets.newHashSet(masterHostGroup, slaveHostGroup), 50, ambariClient,
                removableNodes);
    }

    @Test
    public void testVerifyNodesAreRemovableWithReplicationFactoryVerificationFailBecauseReplication() {

        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";
        String replication = "3";

        Blueprint blueprint = new Blueprint();
        blueprint.setName(ambariName);
        blueprint.setStackName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setBlueprint(blueprint);

        Stack stack = new Stack();
        stack.setCluster(cluster);
        stack.setGatewayPort(gatewayPort);
        stack.setId(100L);
        stack.setPlatformVariant("GCP");

        InstanceGroup masterInstanceGroup = getMasterInstanceGroup();
        InstanceGroup slaveInstanceGroup = getSlaveInstanceGroup(10);
        stack.setInstanceGroups(Sets.newHashSet(masterInstanceGroup, slaveInstanceGroup));

        HostGroup masterHostGroup = getHostGroupForInstanceGroup(masterInstanceGroup, 1L);
        HostGroup slaveHostGroup = getHostGroupForInstanceGroup(slaveInstanceGroup, 2L);
        cluster.setHostGroups(Sets.newHashSet(masterHostGroup, slaveHostGroup));

        Map<String, List<String>> blueprintMap = new HashMap<>();
        blueprintMap.put(slaveHostGroup.getName(), Collections.singletonList("DATANODE"));

        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);
        when(configurationService.getConfiguration(ambariClient, slaveHostGroup.getName()))
                .thenReturn(Collections.singletonMap(ConfigParam.DFS_REPLICATION.key(), replication));

        List<InstanceMetaData> removableNodes =
                slaveInstanceGroup.getAllInstanceMetaData().stream()
                        .filter(instanceMetaData -> instanceMetaData.getPrivateId() < 9L)
                        .collect(Collectors.toList());

        Set<String> removableHostnames = removableNodes.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toSet());
        doAnswer(invocation -> slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> removableHostnames.contains(hostMetadata.getHostName()))
                .collect(Collectors.toList())).when(hostFilterService).filterHostsForDecommission(any(), any(), any(), any(), any());

        Multimap<Long, HostMetadata> hostGroupWithInstances = ArrayListMultimap.create();
        slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> removableHostnames.contains(hostMetadata.getHostName()))
                .forEach(hostMetadata -> {
                    hostGroupWithInstances.put(slaveHostGroup.getId(), hostMetadata);
                });

        thrown.expect(NotEnoughNodeException.class);
        thrown.expectMessage("There is not enough node to downscale. Check the replication factor and the ApplicationMaster occupation.");

        underTest.verifyNodesAreRemovable(stack, hostGroupWithInstances, Sets.newHashSet(masterHostGroup, slaveHostGroup), 50, ambariClient,
                removableNodes);
    }

    @Test
    public void testVerifyNodesAreRemovableWithoutReplicationFactory() {

        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";
        String replication = "0";

        Blueprint blueprint = new Blueprint();
        blueprint.setName(ambariName);
        blueprint.setStackName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setBlueprint(blueprint);

        Stack stack = new Stack();
        stack.setCluster(cluster);
        stack.setGatewayPort(gatewayPort);
        stack.setId(100L);
        stack.setPlatformVariant("GCP");

        InstanceGroup masterInstanceGroup = getMasterInstanceGroup();
        InstanceGroup slaveInstanceGroup = getSlaveInstanceGroup(10);
        stack.setInstanceGroups(Sets.newHashSet(masterInstanceGroup, slaveInstanceGroup));

        HostGroup masterHostGroup = getHostGroupForInstanceGroup(masterInstanceGroup, 1L);
        HostGroup slaveHostGroup = getHostGroupForInstanceGroup(slaveInstanceGroup, 2L);
        cluster.setHostGroups(Sets.newHashSet(masterHostGroup, slaveHostGroup));

        Map<String, List<String>> blueprintMap = new HashMap<>();
        blueprintMap.put(slaveHostGroup.getName(), Collections.singletonList("DATANODE"));

        AmbariClient ambariClient = mock(AmbariClient.class);

        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);
        when(configurationService.getConfiguration(ambariClient, slaveHostGroup.getName()))
                .thenReturn(Collections.singletonMap(ConfigParam.DFS_REPLICATION.key(), replication));
        when(ambariClientPollingService.pollWithTimeoutSingleFailure(any(), any(), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS);

        List<InstanceMetaData> removableNodes =
                slaveInstanceGroup.getAllInstanceMetaData().stream()
                        .filter(instanceMetaData -> instanceMetaData.getPrivateId() < 9L)
                        .collect(Collectors.toList());

        Set<String> removableHostnames = removableNodes.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toSet());
        doAnswer(invocation -> slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> removableHostnames.contains(hostMetadata.getHostName()))
                .collect(Collectors.toList())).when(hostFilterService).filterHostsForDecommission(any(), any(), any(), any(), any());
        Multimap<Long, HostMetadata> hostGroupWithInstances = ArrayListMultimap.create();
        slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> removableHostnames.contains(hostMetadata.getHostName()))
                .forEach(hostMetadata -> {
                    hostGroupWithInstances.put(slaveHostGroup.getId(), hostMetadata);
                });

        underTest.verifyNodesAreRemovable(stack, hostGroupWithInstances, Sets.newHashSet(masterHostGroup, slaveHostGroup), 50, ambariClient,
                removableNodes);

        verify(ambariDecommissionTimeCalculator).calculateDecommissioningTime(any(), any(), any(), anyLong(), anyInt());
    }

    private HostGroup getHostGroupForInstanceGroup(InstanceGroup instanceGroup, Long id) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(instanceGroup.getGroupName());
        hostGroup.setId(id);
        Set<HostMetadata> hostMetadataSet = instanceGroup.getInstanceMetaDataSet().stream().map(instanceMetaData -> {
            HostMetadata hostMetadata = new HostMetadata();
            hostMetadata.setHostName(instanceMetaData.getDiscoveryFQDN());
            hostMetadata.setHostMetadataState(HostMetadataState.HEALTHY);
            hostMetadata.setHostGroup(hostGroup);
            return hostMetadata;
        }).collect(Collectors.toSet());
        hostGroup.setHostMetadata(hostMetadataSet);
        return hostGroup;
    }

    private InstanceGroup getMasterInstanceGroup() {
        InstanceGroup masterInstanceGroup = new InstanceGroup();
        masterInstanceGroup.setGroupName("master_1");
        masterInstanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        masterInstanceGroup.setInstanceMetaData(new HashSet<>());
        for (int i = 0; i < 5; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
            instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
            instanceMetaData.setAmbariServer(true);
            instanceMetaData.setDiscoveryFQDN("10-0-0-" + i + ".example.com");
            instanceMetaData.setPrivateIp("10.0.0." + i);
            instanceMetaData.setPrivateId(1000L + i);
            instanceMetaData.setInstanceGroup(masterInstanceGroup);
            masterInstanceGroup.getInstanceMetaDataSet().add(instanceMetaData);
        }
        return masterInstanceGroup;
    }

    private InstanceGroup getSlaveInstanceGroup(int nodeCount) {
        InstanceGroup slaveInstanceGroup = new InstanceGroup();
        slaveInstanceGroup.setGroupName("slave_1");
        slaveInstanceGroup.setInstanceGroupType(InstanceGroupType.CORE);
        slaveInstanceGroup.setInstanceMetaData(new HashSet<>());
        for (int i = 0; i < nodeCount; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
            instanceMetaData.setInstanceMetadataType(InstanceMetadataType.CORE);
            instanceMetaData.setAmbariServer(false);
            instanceMetaData.setDiscoveryFQDN("10-0-1-" + i + ".example.com");
            instanceMetaData.setPrivateIp("10.0.1." + i);
            instanceMetaData.setPrivateId((long) i);
            instanceMetaData.setInstanceGroup(slaveInstanceGroup);
            slaveInstanceGroup.getInstanceMetaDataSet().add(instanceMetaData);
        }
        return slaveInstanceGroup;
    }

    protected HostMetadata getHostMetadata(Long id) {
        HostMetadata hostMetadata = new HostMetadata();
        hostMetadata.setId(id);
        return hostMetadata;
    }

    private HostMetadata getHostMetadata(String hostname2, HostMetadataState state) {
        HostMetadata healhtyNode = new HostMetadata();
        healhtyNode.setHostName(hostname2);
        healhtyNode.setHostMetadataState(state);
        return healhtyNode;
    }
}
