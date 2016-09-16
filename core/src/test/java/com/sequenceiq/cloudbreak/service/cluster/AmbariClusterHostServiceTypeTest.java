package com.sequenceiq.cloudbreak.service.cluster;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;

import groovyx.net.http.HttpResponseException;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterHostServiceTypeTest {

    @Mock
    private StackService stackService;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @InjectMocks
    @Spy
    private AmbariClusterService underTest = new AmbariClusterService();

    private Stack stack;

    private ClusterRequest clusterRequest;

    private ClusterResponse clusterResponse;

    private Cluster cluster;

    @Before
    public void setUp() throws CloudbreakSecuritySetupException {
        stack = TestUtil.stack();
        cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        clusterRequest = new ClusterRequest();
        clusterResponse = new ClusterResponse();
        when(stackService.get(anyLong())).thenReturn(stack);
        when(stackService.getById(anyLong())).thenReturn(stack);
        when(stackService.findLazy(anyLong())).thenReturn(stack);
        when(clusterRepository.save(any(Cluster.class))).thenReturn(cluster);
        given(tlsSecurityService.buildTLSClientConfig(anyLong(), anyString())).willReturn(new HttpClientConfig("", 8443, "/tmp"));
    }

    @Test(expected = BadRequestException.class)
    public void testStopWhenAwsHasEphemeralVolume() {
        cluster = TestUtil.cluster(TestUtil.blueprint(), TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential()), 1L);
        cluster.getStack().setCloudPlatform("AWS");
        stack = TestUtil.setEphemeral(cluster.getStack());
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStack(stack);
        stack.setCluster(cluster);

        when(stackService.get(anyLong())).thenReturn(stack);
        when(stackService.getById(anyLong())).thenReturn(stack);

        underTest.updateStatus(1L, StatusRequest.STOPPED);
    }

    @Test(expected = BadRequestException.class)
    public void testStopWhenAwsHasSpotInstances() {
        cluster = TestUtil.cluster(TestUtil.blueprint(), TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential()), 1L);
        cluster.getStack().setCloudPlatform("AWS");
        stack = TestUtil.setSpotInstances(cluster.getStack());
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStack(stack);
        stack.setCluster(cluster);

        when(stackService.get(anyLong())).thenReturn(stack);
        when(stackService.getById(anyLong())).thenReturn(stack);

        underTest.updateStatus(1L, StatusRequest.STOPPED);
    }

    @Test(expected = BadRequestException.class)
    public void testRetrieveClusterJsonWhenClusterJsonIsNull() throws HttpResponseException {
        // GIVEN
        doReturn(ambariClient).when(ambariClientProvider).getAmbariClient(any(HttpClientConfig.class), anyInt(), any(String.class), any(String.class));
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
        when(ambariClientProvider.getAmbariClient(any(HttpClientConfig.class), anyInt(), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);
        when(statusToPollGroupConverter.convert(Mockito.any(Status.class))).thenReturn(PollGroup.POLLABLE);

        underTest.updateHosts(stack.getId(), json);
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
        when(ambariClientProvider.getAmbariClient(any(HttpClientConfig.class), anyInt(), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);
        when(statusToPollGroupConverter.convert(Mockito.any(Status.class))).thenReturn(PollGroup.POLLABLE);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
    }

    @Test
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
        when(ambariClientProvider.getAmbariClient(any(HttpClientConfig.class), anyInt(), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(ambariClient.getBlueprintMap(cluster.getBlueprint().getBlueprintName())).thenReturn(singletonMap("slave_1", singletonList("DATANODE")));
        when(ambariClient.getDFSSpace()).thenReturn(dfsSpace);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node1")).thenReturn(instanceMetaData1);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node2")).thenReturn(instanceMetaData2);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node3")).thenReturn(instanceMetaData3);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node4")).thenReturn(instanceMetaData4);
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);
        when(statusToPollGroupConverter.convert(Mockito.any(Status.class))).thenReturn(PollGroup.POLLABLE);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
    }

    @Test
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
        when(ambariClientProvider.getAmbariClient(any(HttpClientConfig.class), anyInt(), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(ambariClient.getBlueprintMap(cluster.getBlueprint().getBlueprintName())).thenReturn(singletonMap("slave_1", singletonList("DATANODE")));
        when(ambariClient.getDFSSpace()).thenReturn(dfsSpace);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node1")).thenReturn(instanceMetaData1);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node2")).thenReturn(instanceMetaData2);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node3")).thenReturn(instanceMetaData3);
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);
        when(statusToPollGroupConverter.convert(Mockito.any(Status.class))).thenReturn(PollGroup.POLLABLE);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
    }

    @Test
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
        when(ambariClientProvider.getAmbariClient(any(HttpClientConfig.class), anyInt(), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(ambariClient.getBlueprintMap(cluster.getBlueprint().getBlueprintName())).thenReturn(singletonMap("slave_1", singletonList("DATANODE")));
        when(ambariClient.getDFSSpace()).thenReturn(dfsSpace);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node1")).thenReturn(instanceMetaData1);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node2")).thenReturn(instanceMetaData2);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node3")).thenReturn(instanceMetaData3);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node4")).thenReturn(instanceMetaData3);
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);
        when(statusToPollGroupConverter.convert(Mockito.any(Status.class))).thenReturn(PollGroup.POLLABLE);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
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
        when(ambariClientProvider.getAmbariClient(any(HttpClientConfig.class), anyInt(), any(String.class), any(String.class))).thenReturn(ambariClient);
        when(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"));
        when(ambariClient.getBlueprintMap(cluster.getBlueprint().getBlueprintName())).thenReturn(singletonMap("slave_1", singletonList("DATANODE")));
        when(ambariClient.getDFSSpace()).thenReturn(dfsSpace);
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node1")).thenReturn(instanceMetaData1);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node2")).thenReturn(instanceMetaData2);
        when(instanceMetadataRepository.findHostInStack(stack.getId(), "node3")).thenReturn(instanceMetaData3);
        when(statusToPollGroupConverter.convert(Mockito.any(Status.class))).thenReturn(PollGroup.POLLABLE);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
    }
}
