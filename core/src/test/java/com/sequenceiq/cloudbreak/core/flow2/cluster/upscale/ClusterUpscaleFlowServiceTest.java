package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_NODES_MARKED_AS_ZOMBIE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_NODE_FAILURE_REASON_NO_FQDN;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class ClusterUpscaleFlowServiceTest {

    private static final long STACK_ID = 1L;

    private static final String HOST_GROUP = "compute";

    private static final String INSTANCE_ID = "i-123";

    private static final String ZOMBIE_REASON = "Cluster upscale failed. Host does not have FQDN.";

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @InjectMocks
    private ClusterUpscaleFlowService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(messagesService.getMessage(CLUSTER_NODE_FAILURE_REASON_NO_FQDN.getMessage())).thenReturn(ZOMBIE_REASON);
    }

    @Test
    void testClusterUpscaleFinishedWhenNodeHasNoFqdnThenMarkedZombieAndCustomerNotified() {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        InstanceMetadataView instanceWithoutFqdn = instanceMetadataView(null, INSTANCE_ID);
        InstanceGroupDto instanceGroupDto = instanceGroupDto(instanceWithoutFqdn);
        when(stackDtoService.getInstanceMetadataByInstanceGroup(STACK_ID)).thenReturn(List.of(instanceGroupDto));

        underTest.clusterUpscaleFinished(stack, Set.of(HOST_GROUP), false);

        verify(instanceMetaDataService, times(1)).updateInstanceStatus(eq(instanceWithoutFqdn), eq(InstanceStatus.ZOMBIE), eq(ZOMBIE_REASON));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_NODES_MARKED_AS_ZOMBIE), eq(INSTANCE_ID), eq(ZOMBIE_REASON));
    }

    @Test
    void testClusterUpscaleFinishedWhenRepairThenOrchestrationFailedAndNoZombieNotification() {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        InstanceMetadataView instanceWithoutFqdn = instanceMetadataView(null, INSTANCE_ID);
        InstanceGroupDto instanceGroupDto = instanceGroupDto(instanceWithoutFqdn);
        when(stackDtoService.getInstanceMetadataByInstanceGroup(STACK_ID)).thenReturn(List.of(instanceGroupDto));

        underTest.clusterUpscaleFinished(stack, Set.of(HOST_GROUP), true);

        verify(instanceMetaDataService, times(1)).updateInstanceStatus(eq(instanceWithoutFqdn), eq(InstanceStatus.ORCHESTRATION_FAILED), eq(ZOMBIE_REASON));
        verify(flowMessageService, never()).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(CLUSTER_NODES_MARKED_AS_ZOMBIE), eq(INSTANCE_ID),
                eq(ZOMBIE_REASON));
    }

    @Test
    void testClusterUpscaleFinishedWhenAllNodesHaveFqdnThenNoZombieNotification() {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        InstanceMetadataView instanceWithFqdn = instanceMetadataView("host.example.com", INSTANCE_ID);
        InstanceGroupDto instanceGroupDto = instanceGroupDto(instanceWithFqdn);
        when(stackDtoService.getInstanceMetadataByInstanceGroup(STACK_ID)).thenReturn(List.of(instanceGroupDto));

        underTest.clusterUpscaleFinished(stack, Set.of(HOST_GROUP), false);

        verify(instanceMetaDataService, never()).updateInstanceStatus(eq(instanceWithFqdn), eq(InstanceStatus.ZOMBIE), eq(ZOMBIE_REASON));
        verify(flowMessageService, never()).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(CLUSTER_NODES_MARKED_AS_ZOMBIE), eq(INSTANCE_ID),
                eq(ZOMBIE_REASON));
    }

    private InstanceGroupDto instanceGroupDto(InstanceMetadataView... instances) {
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        when(instanceGroupView.getGroupName()).thenReturn(HOST_GROUP);
        return new InstanceGroupDto(instanceGroupView, List.of(instances));
    }

    private InstanceMetadataView instanceMetadataView(String discoveryFqdn, String instanceId) {
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        lenient().when(instanceMetadataView.getDiscoveryFQDN()).thenReturn(discoveryFqdn);
        lenient().when(instanceMetadataView.getInstanceId()).thenReturn(instanceId);
        return instanceMetadataView;
    }
}
