package com.sequenceiq.cloudbreak.service.cluster;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.blueprint.validation.AmbariBlueprintValidator;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

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
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Mock
    private AmbariBlueprintValidator ambariBlueprintValidator;

    @Mock
    private BlueprintService blueprintService;

    private Stack stack;

    private Cluster cluster;

    @Before
    public void setUp() {
        stack = TestUtil.stack();
        cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        when(stackService.getById(anyLong())).thenReturn(stack);
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(blueprintService.isAmbariBlueprint(any(Blueprint.class))).thenReturn(Boolean.TRUE);
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
    public void testUpdateHostsDoesntAcceptZeroScalingAdjustments() {
        // GIVEN
        HostGroupAdjustmentV4Request hga1 = new HostGroupAdjustmentV4Request();
        hga1.setHostGroup("slave_1");
        hga1.setScalingAdjustment(0);

        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(new HostGroup()));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("No scaling adjustments specified. Nothing to do.");
        // WHEN
        underTest.updateHosts(stack.getId(), hga1);
    }

    @Test
    public void testUpdateHostsDoesntAcceptScalingAdjustmentsWithDifferentSigns() {
        // GIVEN
        HostGroupAdjustmentV4Request hga1 = new HostGroupAdjustmentV4Request();
        hga1.setHostGroup("slave_1");
        hga1.setScalingAdjustment(-2);

        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(new HostGroup()));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("The host group must contain at least 1 host after the decommission: [hostGroup: 'slave_1', current hosts: 0, "
                + "decommissions requested: 2]");
        // WHEN
        underTest.updateHosts(stack.getId(), hga1);
    }

    @Test
    public void testUpdateHostsForDownscaleFilterAllHosts() {
        HostGroupAdjustmentV4Request json = new HostGroupAdjustmentV4Request();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        Set<HostMetadata> hostsMetaData = new HashSet<>(asList(metadata1, metadata2, metadata3));
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(hostGroup));

        underTest.updateHosts(stack.getId(), json);
    }

    @Test
    public void testUpdateHostsForDownscaleCannotGoBelowReplication() {
        HostGroupAdjustmentV4Request json = new HostGroupAdjustmentV4Request();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3);
        Set<HostMetadata> hostsMetaData = new HashSet<>(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(hostGroup));

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(ambariBlueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup,
                json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleFilterOneHost() {
        HostGroupAdjustmentV4Request json = new HostGroupAdjustmentV4Request();
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
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(hostGroup));

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(ambariBlueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup,
                json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleSelectNodesWithLessData() {
        HostGroupAdjustmentV4Request json = new HostGroupAdjustmentV4Request();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3);
        Set<HostMetadata> hostsMetaData = new HashSet<>(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(hostGroup));

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(ambariBlueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup,
                json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleSelectMultipleNodesWithLessData() {
        HostGroupAdjustmentV4Request json = new HostGroupAdjustmentV4Request();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-2);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        HostMetadata metadata4 = mock(HostMetadata.class);
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3, metadata4);
        Set<HostMetadata> hostsMetaData = new HashSet<>(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(hostGroup));

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(ambariBlueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup,
                json.getScalingAdjustment());
    }

    @Test
    public void testUpdateHostsForDownscaleWhenRemainingSpaceIsNotEnough() {
        HostGroupAdjustmentV4Request json = new HostGroupAdjustmentV4Request();
        json.setHostGroup("slave_1");
        json.setScalingAdjustment(-1);
        HostMetadata metadata1 = mock(HostMetadata.class);
        HostMetadata metadata2 = mock(HostMetadata.class);
        HostMetadata metadata3 = mock(HostMetadata.class);
        List<HostMetadata> hostsMetadataList = asList(metadata1, metadata2, metadata3);
        Set<HostMetadata> hostsMetaData = new HashSet<>(hostsMetadataList);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setHostMetadata(hostsMetaData);
        hostGroup.setName("slave_1");
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(hostGroup));

        underTest.updateHosts(stack.getId(), json);

        verify(flowManager, times(1)).triggerClusterDownscale(stack.getId(), json);
        verify(ambariBlueprintValidator, times(1)).validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup,
                json.getScalingAdjustment());
    }
}
