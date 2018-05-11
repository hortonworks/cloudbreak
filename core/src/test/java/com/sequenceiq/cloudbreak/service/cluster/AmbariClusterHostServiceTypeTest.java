package com.sequenceiq.cloudbreak.service.cluster;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import groovyx.net.http.HttpResponseException;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterHostServiceTypeTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    @Spy
    private final AmbariClusterService underTest = new AmbariClusterService();

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

    @Mock
    private BlueprintValidator blueprintValidator;

    private Stack stack;

    private Cluster cluster;

    @Before
    public void setUp() {
        stack = TestUtil.stack();
        cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        when(stackService.get(anyLong())).thenReturn(stack);
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        given(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(anyLong(), anyString())).willReturn(new HttpClientConfig("", "", "/tmp", "/tmp"));
    }

    @Test
    public void testStopWhenAwsHasEphemeralVolume() {
        cluster = TestUtil.cluster(TestUtil.blueprint(), TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential()), 1L);
        cluster.getStack().setCloudPlatform("AWS");
        stack = TestUtil.setEphemeral(cluster.getStack());
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStack(stack);
        stack.setCluster(cluster);

        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot stop a cluster '1'. Reason: Instances with ephemeral volumes cannot be stopped.");

        underTest.updateStatus(1L, StatusRequest.STOPPED);
    }

    @Test
    public void testStopWhenAwsHasSpotInstances() {
        cluster = TestUtil.cluster(TestUtil.blueprint(), TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential()), 1L);
        cluster.getStack().setCloudPlatform("AWS");
        stack = TestUtil.setSpotInstances(cluster.getStack());
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStack(stack);
        stack.setCluster(cluster);

        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot stop a cluster '1'. Reason: Spot instances cannot be stopped.");

        underTest.updateStatus(1L, StatusRequest.STOPPED);
    }

    @Test
    public void testRetrieveClusterJsonWhenClusterJsonIsNull() throws HttpResponseException {
        // GIVEN
        doReturn(ambariClient).when(ambariClientProvider).getAmbariClient(any(HttpClientConfig.class), nullable(Integer.class), any(Cluster.class));
        given(ambariClient.getClusterAsJson()).willReturn(null);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cluster response coming from Ambari server was null. [Ambari Server IP: '123.12.3.4']");
        // WHEN
        underTest.getClusterJson("123.12.3.4", 1L);
    }

    @Test
    public void testUpdateHostsDoesntAcceptZeroScalingAdjustments() throws Exception {
        // GIVEN
        HostGroupAdjustmentJson hga1 = new HostGroupAdjustmentJson();
        hga1.setHostGroup("slave_1");
        hga1.setScalingAdjustment(0);

        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(new HostGroup());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("No scaling adjustments specified. Nothing to do.");
        // WHEN
        underTest.updateHosts(stack.getId(), hga1);
    }

    @Test
    public void testUpdateHostsDoesntAcceptScalingAdjustmentsWithDifferentSigns() throws Exception {
        // GIVEN
        HostGroupAdjustmentJson hga1 = new HostGroupAdjustmentJson();
        hga1.setHostGroup("slave_1");
        hga1.setScalingAdjustment(-2);

        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(new HostGroup());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("The host group must contain at least 1 host after the decommission: [hostGroup: 'slave_1', current hosts: 0, "
                + "decommissions requested: 2]");
        // WHEN
        underTest.updateHosts(stack.getId(), hga1);
    }

    @Test
    public void testUpdateHostsForDownscaleFilterAllHosts() throws CloudbreakSecuritySetupException {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>(asList(metadata1, metadata2, metadata3));
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);
    }

    @Test
    public void testUpdateHostsForDownscaleCannotGoBelowReplication() throws CloudbreakSecuritySetupException {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>();
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3);
        hostsMetaData.addAll(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(blueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleFilterOneHost() throws CloudbreakSecuritySetupException {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        HostMetadata metadata4 = mock(HostMetadata.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>(asList(metadata1, metadata2, metadata3, metadata4));
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(blueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleSelectNodesWithLessData() throws CloudbreakSecuritySetupException {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>();
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3);
        hostsMetaData.addAll(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(blueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleSelectMultipleNodesWithLessData() throws Exception {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-2);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        HostMetadata metadata4 = mock(HostMetadata.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>();
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3, metadata4);
        hostsMetaData.addAll(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(blueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleWhenRemainingSpaceIsNotEnough() throws Exception {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>();
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3);
        hostsMetaData.addAll(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(blueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, json.getScalingAdjustment());
    }
}
