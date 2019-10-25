package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
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
    private final ClusterService underTest = new ClusterService();

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
        when(stackService.getById(anyLong())).thenReturn(stack);
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
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

        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);

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

        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);

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
    public void testUpdateHostsDoesntAcceptZeroScalingAdjustments() {
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
    public void testUpdateHostsDoesntAcceptScalingAdjustmentsWithDifferentSigns() {
        // GIVEN
        HostGroupAdjustmentJson hga1 = new HostGroupAdjustmentJson();
        hga1.setHostGroup("slave_1");
        hga1.setScalingAdjustment(-2);

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("slave_1");
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, null, 0);
        hostGroup.setInstanceGroup(instanceGroup);
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("The host group must contain at least 1 host after the decommission: [hostGroup: 'slave_1', current hosts: 0, "
                + "decommissions requested: 2]");
        // WHEN
        underTest.updateHosts(stack.getId(), hga1);
    }

    @Test
    public void testUpdateHostsForDownscaleCannotGoBelowReplication() {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("slave_1");
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, null, 2);
        hostGroup.setInstanceGroup(instanceGroup);
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(blueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleFilterOneHost() {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("slave_1");
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, null, 4);
        hostGroup.setInstanceGroup(instanceGroup);
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(blueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleSelectNodesWithLessData() {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("slave_1");
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, null, 3);
        hostGroup.setInstanceGroup(instanceGroup);
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(blueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleSelectMultipleNodesWithLessData() {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-2);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("slave_1");
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, null, 4);
        hostGroup.setInstanceGroup(instanceGroup);
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(blueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleWhenRemainingSpaceIsNotEnough() {
        HostGroupAdjustmentJson json = new HostGroupAdjustmentJson();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("slave_1");
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, null, 3);
        hostGroup.setInstanceGroup(instanceGroup);
        when(hostGroupService.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup);

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(blueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, json.getScalingAdjustment());
    }
}
