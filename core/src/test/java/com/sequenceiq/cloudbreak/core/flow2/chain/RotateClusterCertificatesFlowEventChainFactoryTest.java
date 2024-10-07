package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CMCA_ROTATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent.STOPSTART_UPSCALE_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCertificatesRotationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
class RotateClusterCertificatesFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private RotateClusterCertificatesFlowEventChainFactory underTest;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Test
    public void testChainQueueForCertificateRotationWithStoppedNodes() {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.STOPPED, InstanceGroupType.CORE);
        host1.setClusterManagerServer(true);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        HostGroup hostGroup2 = new HostGroup();
        hostGroup2.setName("hostGroup2");
        hostGroup2.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host2 = getHost("host2", hostGroup2.getName(), InstanceStatus.STOPPED, InstanceGroupType.CORE);
        hostGroup2.setInstanceGroup(host2.getInstanceGroup());

        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of(host1, host2));
        ClusterCertificatesRotationTriggerEvent event = new ClusterCertificatesRotationTriggerEvent(
                CLUSTER_CMCA_ROTATION_EVENT.event(),
                STACK_ID,
                CertificateRotationType.HOST_CERTS,
                true);
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(2, flowChainQueue.getQueue().size());
        assertClusterScaleTriggerEvent(flowChainQueue);
    }

    @Test
    public void testChainQueueForCertificateRotationWithNoStoppedNodes() {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_RUNNING, InstanceGroupType.CORE);
        host1.setClusterManagerServer(true);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        HostGroup hostGroup2 = new HostGroup();
        hostGroup2.setName("hostGroup2");
        hostGroup2.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host2 = getHost("host2", hostGroup2.getName(), InstanceStatus.SERVICES_RUNNING, InstanceGroupType.CORE);
        hostGroup2.setInstanceGroup(host2.getInstanceGroup());

        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of(host1, host2));
        ClusterCertificatesRotationTriggerEvent event = new ClusterCertificatesRotationTriggerEvent(
                CLUSTER_CMCA_ROTATION_EVENT.event(),
                STACK_ID,
                CertificateRotationType.HOST_CERTS,
                true);
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(1, flowChainQueue.getQueue().size());
        assertEquals(CLUSTER_CMCA_ROTATION_EVENT.event(), flowChainQueue.getQueue().remove().selector());
    }

    private void assertClusterScaleTriggerEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable clusterScaleEvent = flowChainQueue.getQueue().remove();
        assertEquals(STOPSTART_UPSCALE_TRIGGER_EVENT.event(), clusterScaleEvent.selector());
        assertEquals(STACK_ID, clusterScaleEvent.getResourceId());
        assertInstanceOf(StopStartUpscaleTriggerEvent.class, clusterScaleEvent);
    }

    private InstanceMetaData getHost(String hostName, String groupName, InstanceStatus instanceStatus, InstanceGroupType instanceGroupType) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(instanceGroupType);
        instanceGroup.setGroupName(groupName);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(hostName);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setInstanceStatus(instanceStatus);
        instanceMetaData.setInstanceId(hostName);
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));

        return instanceMetaData;
    }
}