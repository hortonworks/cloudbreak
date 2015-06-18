package com.sequenceiq.cloudbreak.service.cluster;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.cluster.filter.HostFilterService;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;

import groovyx.net.http.HttpResponseException;
import reactor.bus.Event;
import reactor.bus.EventBus;
public class AmbariClusterServiceTest {

    @InjectMocks
    @Spy
    private AmbariClusterService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private EventBus reactor;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private HttpResponseException mockedException;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private HostFilterService hostFilterService;

    @Mock
    private HostGroupRepository hostGroupRepository;

    @Mock
    private AmbariConfigurationService configurationService;

    @Mock
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Captor
    private ArgumentCaptor<Event<UpdateAmbariHostsRequest>> eventCaptor;

    @Captor
    private ArgumentCaptor<String> eventTypeCaptor;

    private Stack stack;

    private ClusterRequest clusterRequest;

    private ClusterResponse clusterResponse;

    private Cluster cluster;

    @Before
    public void setUp() throws CloudbreakSecuritySetupException {
        underTest = new AmbariClusterService();
        MockitoAnnotations.initMocks(this);
        cluster = createCluster();
        stack = createStack(cluster);
        clusterRequest = new ClusterRequest();
        clusterResponse = new ClusterResponse();
        given(stackRepository.findById(anyLong())).willReturn(stack);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(tlsSecurityService.buildTLSClientConfig(anyLong(), anyString())).willReturn(new TLSClientConfig("", "/tmp"));
    }

    @Test(expected = BadRequestException.class)
    public void testRetrieveClusterJsonWhenClusterJsonIsNull() throws HttpResponseException {
        // GIVEN
        doReturn(ambariClient).when(ambariClientProvider).getAmbariClient(any(TLSClientConfig.class), any(String.class), any(String.class));
        given(ambariClient.getClusterAsJson()).willReturn(null);
        // WHEN
        underTest.getClusterJson("123.12.3.4", 1L);
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateHostsDoesntAcceptZeroScalingAdjustments() throws Exception {
        // GIVEN
        HostGroupAdjustmentJson hga1 = new HostGroupAdjustmentJson();
        hga1.setHostGroup("slave_1");
        hga1.setScalingAdjustment(0);
        // WHEN
        underTest.updateHosts(stack.getId(), hga1);
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateHostsDoesntAcceptScalingAdjustmentsWithDifferentSigns() throws Exception {
        // GIVEN
        HostGroupAdjustmentJson hga1 = new HostGroupAdjustmentJson();
        hga1.setHostGroup("slave_1");
        hga1.setScalingAdjustment(-2);
        // WHEN
        underTest.updateHosts(stack.getId(), hga1);
    }

    @Test
    public void testUpdateHostsForDownscaleFilterAllHosts() throws ConnectException, CloudbreakSecuritySetupException {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        AmbariClient ambariClient = mock(AmbariClient.class);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>();
        hostsMetaData.addAll(asList(metadata1, metadata2, metadata3));
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), "slave_1")).thenReturn(hostGroup);
        when(ambariClientProvider.getAmbariClient(any(TLSClientConfig.class), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(configurationService.getConfiguration(ambariClient, "slave_1")).thenReturn(singletonMap(ConfigParam.DFS_REPLICATION.key(), "2"));
        when(hostFilterService.filterHostsForDecommission(stack, hostsMetaData, "slave_1")).thenReturn(Collections.<HostMetadata>emptyList());

        Exception result = null;
        try {
            underTest.updateHosts(stack.getId(), json);
        } catch (BadRequestException e) {
            result = e;
        }

        assertTrue(result.getMessage().startsWith("There is not enough node to downscale."));
    }

    @Test
    public void testUpdateHostsForDownscaleCannotGoBelowReplication() throws ConnectException, CloudbreakSecuritySetupException {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        AmbariClient ambariClient = mock(AmbariClient.class);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>();
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3);
        hostsMetaData.addAll(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), "slave_1")).thenReturn(hostGroup);
        when(ambariClientProvider.getAmbariClient(any(TLSClientConfig.class), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(configurationService.getConfiguration(ambariClient, "slave_1")).thenReturn(singletonMap(ConfigParam.DFS_REPLICATION.key(), "2"));
        when(hostFilterService.filterHostsForDecommission(stack, hostsMetaData, "slave_1")).thenReturn(asList(metadata2, metadata3));

        Exception result = null;
        try {
            underTest.updateHosts(stack.getId(), json);
        } catch (BadRequestException e) {
            result = e;
        }

        assertTrue(result.getMessage().startsWith("There is not enough node to downscale."));
    }

    @Test
    @Ignore("Rewrite the test not to use reactor!")
    public void testUpdateHostsForDownscaleFilterOneHost() throws ConnectException, CloudbreakSecuritySetupException {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        AmbariClient ambariClient = mock(AmbariClient.class);
        HostMetadata metadata1 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData1 = mock(InstanceMetaData.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData2 = mock(InstanceMetaData.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData3 = mock(InstanceMetaData.class);
        HostMetadata metadata4 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData4 = mock(InstanceMetaData.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>(asList(metadata1, metadata2, metadata3, metadata4));
        List<HostMetadata> hostsMetadataList = asList(metadata2, metadata3, metadata4);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        Map<String, Map<Long, Long>> dfsSpace = new HashMap<>();
        dfsSpace.put("node2", singletonMap(85_000L, 15_000L));
        dfsSpace.put("node1", singletonMap(90_000L, 10_000L));
        dfsSpace.put("node3", singletonMap(80_000L, 20_000L));
        dfsSpace.put("node4", singletonMap(80_000L, 11_000L));
        when(metadata1.getHostName()).thenReturn("node1");
        when(metadata2.getHostName()).thenReturn("node2");
        when(metadata3.getHostName()).thenReturn("node3");
        when(metadata4.getHostName()).thenReturn("node4");
        when(instanceMetaData1.getAmbariServer()).thenReturn(false);
        when(instanceMetaData2.getAmbariServer()).thenReturn(false);
        when(instanceMetaData3.getAmbariServer()).thenReturn(false);
        when(instanceMetaData4.getAmbariServer()).thenReturn(false);
        when(hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), "slave_1")).thenReturn(hostGroup);
        when(ambariClientProvider.getAmbariClient(any(TLSClientConfig.class), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(configurationService.getConfiguration(ambariClient, "slave_1")).thenReturn(singletonMap(ConfigParam.DFS_REPLICATION.key(), "1"));
        when(hostFilterService.filterHostsForDecommission(stack, hostsMetaData, "slave_1")).thenReturn(hostsMetadataList);
        when(ambariClient.getBlueprintMap(cluster.getBlueprint().getBlueprintName())).thenReturn(singletonMap("slave_1", asList("DATANODE")));
        when(ambariClient.getDFSSpace()).thenReturn(dfsSpace);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node1")).thenReturn(instanceMetaData1);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node2")).thenReturn(instanceMetaData2);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node3")).thenReturn(instanceMetaData3);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node4")).thenReturn(instanceMetaData4);

        underTest.updateHosts(stack.getId(), json);

        verify(reactor).notify(eventCaptor.capture(), eventCaptor.capture());
        List<HostMetadata> candidates = eventCaptor.getValue().getData().getDecommissionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("node4", candidates.get(0).getHostName());
    }

    @Test
    @Ignore("Rewrite test not to use reactor!")
    public void testUpdateHostsForDownscaleSelectNodesWithLessData() throws ConnectException, CloudbreakSecuritySetupException {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        AmbariClient ambariClient = mock(AmbariClient.class);
        HostMetadata metadata1 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData1 = mock(InstanceMetaData.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData2 = mock(InstanceMetaData.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData3 = mock(InstanceMetaData.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>();
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3);
        hostsMetaData.addAll(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        Map<String, Map<Long, Long>> dfsSpace = new HashMap<>();
        dfsSpace.put("node2", singletonMap(85_000L, 15_000L));
        dfsSpace.put("node1", singletonMap(90_000L, 10_000L));
        dfsSpace.put("node3", singletonMap(80_000L, 20_000L));
        when(metadata1.getHostName()).thenReturn("node1");
        when(metadata2.getHostName()).thenReturn("node2");
        when(metadata3.getHostName()).thenReturn("node3");
        when(instanceMetaData1.getAmbariServer()).thenReturn(false);
        when(instanceMetaData2.getAmbariServer()).thenReturn(false);
        when(instanceMetaData3.getAmbariServer()).thenReturn(false);
        when(hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), "slave_1")).thenReturn(hostGroup);
        when(ambariClientProvider.getAmbariClient(any(TLSClientConfig.class), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(configurationService.getConfiguration(ambariClient, "slave_1")).thenReturn(singletonMap(ConfigParam.DFS_REPLICATION.key(), "1"));
        when(hostFilterService.filterHostsForDecommission(stack, hostsMetaData, "slave_1")).thenReturn(hostsMetadataList);
        when(ambariClient.getBlueprintMap(cluster.getBlueprint().getBlueprintName())).thenReturn(singletonMap("slave_1", asList("DATANODE")));
        when(ambariClient.getDFSSpace()).thenReturn(dfsSpace);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node1")).thenReturn(instanceMetaData1);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node2")).thenReturn(instanceMetaData2);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node3")).thenReturn(instanceMetaData3);

        underTest.updateHosts(stack.getId(), json);

        verify(reactor).notify(eventCaptor.capture(), eventCaptor.capture());
        List<HostMetadata> candidates = eventCaptor.getValue().getData().getDecommissionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("node1", candidates.get(0).getHostName());
    }

    @Test
    @Ignore("Not to use reactor")
    public void testUpdateHostsForDownscaleSelectMultipleNodesWithLessData() throws Exception {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-2);
        AmbariClient ambariClient = mock(AmbariClient.class);
        HostMetadata metadata1 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData1 = mock(InstanceMetaData.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData2 = mock(InstanceMetaData.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData3 = mock(InstanceMetaData.class);
        HostMetadata metadata4 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData4 = mock(InstanceMetaData.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>();
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3, metadata4);
        hostsMetaData.addAll(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        Map<String, Map<Long, Long>> dfsSpace = new HashMap<>();
        dfsSpace.put("node2", singletonMap(85_000L, 15_000L));
        dfsSpace.put("node1", singletonMap(90_000L, 10_000L));
        dfsSpace.put("node3", singletonMap(80_000L, 20_000L));
        dfsSpace.put("node4", singletonMap(90_000L, 10_000L));
        when(metadata1.getHostName()).thenReturn("node1");
        when(metadata2.getHostName()).thenReturn("node2");
        when(metadata3.getHostName()).thenReturn("node3");
        when(metadata3.getHostName()).thenReturn("node4");
        when(instanceMetaData1.getAmbariServer()).thenReturn(false);
        when(instanceMetaData2.getAmbariServer()).thenReturn(false);
        when(instanceMetaData3.getAmbariServer()).thenReturn(false);
        when(instanceMetaData4.getAmbariServer()).thenReturn(false);
        when(hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), "slave_1")).thenReturn(hostGroup);
        when(ambariClientProvider.getAmbariClient(any(TLSClientConfig.class), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(configurationService.getConfiguration(ambariClient, "slave_1")).thenReturn(singletonMap(ConfigParam.DFS_REPLICATION.key(), "1"));
        when(hostFilterService.filterHostsForDecommission(stack, hostsMetaData, "slave_1")).thenReturn(hostsMetadataList);
        when(ambariClient.getBlueprintMap(cluster.getBlueprint().getBlueprintName())).thenReturn(singletonMap("slave_1", asList("DATANODE")));
        when(ambariClient.getDFSSpace()).thenReturn(dfsSpace);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node1")).thenReturn(instanceMetaData1);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node2")).thenReturn(instanceMetaData2);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node3")).thenReturn(instanceMetaData3);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node4")).thenReturn(instanceMetaData3);

        underTest.updateHosts(stack.getId(), json);

        verify(reactor).notify(eventCaptor.capture(), eventCaptor.capture());
        List<HostMetadata> candidates = eventCaptor.getValue().getData().getDecommissionCandidates();
        assertEquals(2, candidates.size());
        assertEquals("node1", candidates.get(0).getHostName());
        assertEquals("node4", candidates.get(1).getHostName());
    }

    @Test
    public void testUpdateHostsForDownscaleWhenRemainingSpaceIsNotEnough() throws Exception {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        AmbariClient ambariClient = mock(AmbariClient.class);
        HostMetadata metadata1 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData1 = mock(InstanceMetaData.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData2 = mock(InstanceMetaData.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        InstanceMetaData instanceMetaData3 = mock(InstanceMetaData.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>();
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3);
        hostsMetaData.addAll(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        Map<String, Map<Long, Long>> dfsSpace = new HashMap<>();
        dfsSpace.put("node2", singletonMap(5_000L, 15_000L));
        dfsSpace.put("node1", singletonMap(10_000L, 10_000L));
        dfsSpace.put("node3", singletonMap(6_000L, 20_000L));
        when(metadata1.getHostName()).thenReturn("node1");
        when(metadata2.getHostName()).thenReturn("node2");
        when(metadata3.getHostName()).thenReturn("node3");
        when(instanceMetaData1.getAmbariServer()).thenReturn(false);
        when(instanceMetaData2.getAmbariServer()).thenReturn(false);
        when(instanceMetaData3.getAmbariServer()).thenReturn(false);
        when(hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), "slave_1")).thenReturn(hostGroup);
        when(ambariClientProvider.getAmbariClient(any(TLSClientConfig.class), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(configurationService.getConfiguration(ambariClient, "slave_1")).thenReturn(singletonMap(ConfigParam.DFS_REPLICATION.key(), "1"));
        when(hostFilterService.filterHostsForDecommission(stack, hostsMetaData, "slave_1")).thenReturn(hostsMetadataList);
        when(ambariClient.getBlueprintMap(cluster.getBlueprint().getBlueprintName())).thenReturn(singletonMap("slave_1", asList("DATANODE")));
        when(ambariClient.getDFSSpace()).thenReturn(dfsSpace);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node1")).thenReturn(instanceMetaData1);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node2")).thenReturn(instanceMetaData2);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node3")).thenReturn(instanceMetaData3);

        Exception result = null;
        try {
            underTest.updateHosts(stack.getId(), json);
        } catch (BadRequestException e) {
            result = e;
        }

        assertEquals("Trying to move '10000' bytes worth of data to nodes with '11000' bytes of capacity is not allowed", result.getMessage());
    }

    private Stack createStack(Cluster cluster) {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCluster(cluster);
        return stack;
    }

    private Cluster createCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("dummyCluster");
        Blueprint blueprint = new Blueprint();
        cluster.setAmbariIp("52.53.54.100");
        blueprint.setId(1L);
        blueprint.setBlueprintText("{\"host_groups\":[{\"name\":\"slave_1\",\"components\":[{\"name\":\"DATANODE\"}]}]}");
        blueprint.setName("multi-node-yarn");
        cluster.setBlueprint(blueprint);
        return cluster;
    }
}
