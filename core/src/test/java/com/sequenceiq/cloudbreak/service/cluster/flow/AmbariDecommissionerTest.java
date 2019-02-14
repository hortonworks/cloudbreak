package com.sequenceiq.cloudbreak.service.cluster.flow;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import org.mockito.Spy;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.service.cluster.NotRecommendedNodeRemovalException;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariConfigurationService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariDecommissionTimeCalculator;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.cluster.filter.ConfigParam;
import com.sequenceiq.cloudbreak.service.cluster.filter.HostFilterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class AmbariDecommissionerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private final AmbariDecommissioner underTest = new AmbariDecommissioner();

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Spy
    private HostGroupService hostGroupService;

    @Mock
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private OrchestratorTypeResolver orchestratorTypeResolver;

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

    public void testVerifyNodesAreRemovableWithReplicationFactor() {
        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";

        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setName(ambariName);
        clusterDefinition.setStackName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setClusterDefinition(clusterDefinition);

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

        HttpClientConfig config = new HttpClientConfig(ipAddress);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp())).thenReturn(config);
        when(ambariClientProvider.getAmbariClient(config, stack.getGatewayPort(), cluster)).thenReturn(ambariClient);

        doReturn(Sets.newHashSet(masterHostGroup, slaveHostGroup)).when(hostGroupService).getByCluster(nullable(Long.class));
        doAnswer(invocation -> slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> hostMetadata.getHostName().equals(invocation.getArguments()[1]))
                .findFirst().get())
                .when(hostGroupService).getHostMetadataByClusterAndHostName(any(), any());
        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);
        when(configurationService.getConfiguration(ambariClient, slaveHostGroup.getName()))
                .thenReturn(Collections.singletonMap(ConfigParam.DFS_REPLICATION.key(), "3"));
        when(ambariClientPollingService.pollWithTimeoutSingleFailure(any(), any(), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS);

        List<InstanceMetaData> removableNodes =
                slaveInstanceGroup.getAllInstanceMetaData().stream()
                        .filter(instanceMetaData -> instanceMetaData.getPrivateId() < 3L)
                        .collect(Collectors.toList());

        doAnswer(invocation -> {
            List<String> removableFQDNs = removableNodes.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
            return slaveHostGroup.getHostMetadata().stream()
                    .filter(hostMetadata -> removableFQDNs.contains(hostMetadata.getHostName()))
                    .collect(Collectors.toList());
        }).when(hostFilterService).filterHostsForDecommission(any(), any(), any());

        underTest.verifyNodesAreRemovable(stack, removableNodes);

        verify(ambariDecommissionTimeCalculator).calculateDecommissioningTime(any(), any(), any(), anyLong());
    }

    @Test
    public void testVerifyNodesAreRemovableFilterOutNodes() {

        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";

        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setName(ambariName);
        clusterDefinition.setStackName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setClusterDefinition(clusterDefinition);

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

        HttpClientConfig config = new HttpClientConfig(ipAddress);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp())).thenReturn(config);
        when(ambariClientProvider.getAmbariClient(config, stack.getGatewayPort(), cluster)).thenReturn(ambariClient);

        doReturn(Sets.newHashSet(masterHostGroup, slaveHostGroup)).when(hostGroupService).getByCluster(nullable(Long.class));
        doAnswer(invocation -> slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> hostMetadata.getHostName().equals(invocation.getArguments()[1]))
                .findFirst().get())
                .when(hostGroupService).getHostMetadataByClusterAndHostName(any(), any());
        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);
        when(configurationService.getConfiguration(ambariClient, slaveHostGroup.getName()))
                .thenReturn(Collections.singletonMap(ConfigParam.DFS_REPLICATION.key(), "3"));

        List<InstanceMetaData> removableNodes =
                slaveInstanceGroup.getAllInstanceMetaData().stream()
                        .filter(instanceMetaData -> instanceMetaData.getPrivateId() < 3L)
                        .collect(Collectors.toList());

        doAnswer(invocation -> {
            List<String> removableFQDNs = removableNodes.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
            return slaveHostGroup.getHostMetadata().stream()
                    .filter(hostMetadata -> !"10-0-1-0.example.com".equals(hostMetadata.getHostName()))
                    .filter(hostMetadata -> removableFQDNs.contains(hostMetadata.getHostName()))
                    .collect(Collectors.toList());
        }).when(hostFilterService).filterHostsForDecommission(any(), any(), any());

        thrown.expect(NotRecommendedNodeRemovalException.class);
        thrown.expectMessage("Following nodes shouldn't be removed from the cluster: [10-0-1-0.example.com]");

        underTest.verifyNodesAreRemovable(stack, removableNodes);
    }

    @Test
    public void testVerifyNodesAreRemovableWithReplicationFactoryVerificationFailBecauseReplication() {

        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";
        String replication = "3";

        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setName(ambariName);
        clusterDefinition.setStackName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setClusterDefinition(clusterDefinition);

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

        HttpClientConfig config = new HttpClientConfig(ipAddress);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp())).thenReturn(config);
        when(ambariClientProvider.getAmbariClient(config, stack.getGatewayPort(), cluster)).thenReturn(ambariClient);

        doReturn(Sets.newHashSet(masterHostGroup, slaveHostGroup)).when(hostGroupService).getByCluster(nullable(Long.class));
        doAnswer(invocation -> slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> hostMetadata.getHostName().equals(invocation.getArguments()[1]))
                .findFirst().get())
                .when(hostGroupService).getHostMetadataByClusterAndHostName(any(), any());
        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);
        when(configurationService.getConfiguration(ambariClient, slaveHostGroup.getName()))
                .thenReturn(Collections.singletonMap(ConfigParam.DFS_REPLICATION.key(), replication));

        List<InstanceMetaData> removableNodes =
                slaveInstanceGroup.getAllInstanceMetaData().stream()
                        .filter(instanceMetaData -> instanceMetaData.getPrivateId() < 9L)
                        .collect(Collectors.toList());

        doAnswer(invocation -> {
            List<String> removableFQDNs = removableNodes.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
            return slaveHostGroup.getHostMetadata().stream()
                    .filter(hostMetadata -> removableFQDNs.contains(hostMetadata.getHostName()))
                    .collect(Collectors.toList());
        }).when(hostFilterService).filterHostsForDecommission(any(), any(), any());

        thrown.expect(NotEnoughNodeException.class);
        thrown.expectMessage("There is not enough node to downscale. Check the replication factor and the ApplicationMaster occupation.");

        underTest.verifyNodesAreRemovable(stack, removableNodes);
    }

    @Test
    public void testVerifyNodesAreRemovableWithoutReplicationFactory() {

        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";
        String replication = "0";

        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setName(ambariName);
        clusterDefinition.setStackName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setClusterDefinition(clusterDefinition);

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

        HttpClientConfig config = new HttpClientConfig(ipAddress);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp())).thenReturn(config);
        when(ambariClientProvider.getAmbariClient(config, stack.getGatewayPort(), cluster)).thenReturn(ambariClient);

        doReturn(Sets.newHashSet(masterHostGroup, slaveHostGroup)).when(hostGroupService).getByCluster(nullable(Long.class));
        doAnswer(invocation -> slaveHostGroup.getHostMetadata().stream()
                .filter(hostMetadata -> hostMetadata.getHostName().equals(invocation.getArguments()[1]))
                .findFirst().get())
                .when(hostGroupService).getHostMetadataByClusterAndHostName(any(), any());
        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);
        when(configurationService.getConfiguration(ambariClient, slaveHostGroup.getName()))
                .thenReturn(Collections.singletonMap(ConfigParam.DFS_REPLICATION.key(), replication));
        when(ambariClientPollingService.pollWithTimeoutSingleFailure(any(), any(), anyInt(), anyInt())).thenReturn(PollingResult.SUCCESS);

        List<InstanceMetaData> removableNodes =
                slaveInstanceGroup.getAllInstanceMetaData().stream()
                        .filter(instanceMetaData -> instanceMetaData.getPrivateId() < 9L)
                        .collect(Collectors.toList());

        doAnswer(invocation -> {
            List<String> removableFQDNs = removableNodes.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
            return slaveHostGroup.getHostMetadata().stream()
                    .filter(hostMetadata -> removableFQDNs.contains(hostMetadata.getHostName()))
                    .collect(Collectors.toList());
        }).when(hostFilterService).filterHostsForDecommission(any(), any(), any());

        underTest.verifyNodesAreRemovable(stack, removableNodes);

        verify(ambariDecommissionTimeCalculator).calculateDecommissioningTime(any(), any(), any(), anyLong());
    }

    @Test
    public void testRemoveHostsFromOrchestrator() throws CloudbreakException, CloudbreakOrchestratorException {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        stack.setId(100L);
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("SALT");
        stack.setOrchestrator(orchestrator);
        when(orchestratorTypeResolver.resolveType(any(String.class))).thenReturn(OrchestratorType.HOST);
        HostOrchestrator hostOrchestrator = mock(HostOrchestrator.class);
        when(hostOrchestratorResolver.get(any())).thenReturn(hostOrchestrator);

        InstanceGroup masterInstanceGroup = getMasterInstanceGroup();
        InstanceGroup slaveInstanceGroup = getSlaveInstanceGroup(100);
        stack.setInstanceGroups(Sets.newHashSet(masterInstanceGroup, slaveInstanceGroup));

        HostGroup masterHostGroup = getHostGroupForInstanceGroup(masterInstanceGroup, 1L);
        HostGroup slaveHostGroup = getHostGroupForInstanceGroup(slaveInstanceGroup, 2L);
        cluster.setHostGroups(Sets.newHashSet(masterHostGroup, slaveHostGroup));
        underTest.removeHostsFromOrchestrator(stack, Lists.newArrayList("10-0-1-50.example.com", "10-0-1-62.example.com"));

        verify(hostOrchestrator, times(1)).tearDown(any(), stringStringCaptor.capture());
        Map<String, String> privateIPsByFQDN = stringStringCaptor.getValue();
        assertEquals(privateIPsByFQDN.keySet().size(), 2L);
        assertThat(privateIPsByFQDN, hasEntry("10-0-1-50.example.com", "10.0.1.50"));
        assertThat(privateIPsByFQDN, hasEntry("10-0-1-62.example.com", "10.0.1.62"));
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
