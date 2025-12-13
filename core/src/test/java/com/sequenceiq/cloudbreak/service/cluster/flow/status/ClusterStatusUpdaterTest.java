package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_AMBARI_CLUSTER_COULD_NOT_SYNC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class ClusterStatusUpdaterTest {
    private static final Long TEST_STACK_ID = 0L;

    private static final Long TEST_CLUSTER_ID = 0L;

    private static final String TEST_BLUEPRINT = "blueprint";

    private static final String TEST_REASON = "Reason";

    @InjectMocks
    private ClusterStatusUpdater underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterStatusService clusterStatusService;

    @BeforeEach
    void setUp() {
        lenient().when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        lenient().when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        lenient().when(clusterStatusService.getStatus(anyBoolean())).thenReturn(ClusterStatusResult.of(ClusterStatus.STARTED));
    }

    @Test
    void testUpdateClusterStatusShouldUpdateStackStatusWhenStackStatusChanged() {
        // GIVEN
        Stack stack = createStackWithCluster(DetailedStackStatus.NODE_FAILURE);
        when(clusterStatusService.getStatus(anyBoolean())).thenReturn(ClusterStatusResult.of(ClusterStatus.INSTALLED));
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        verify(clusterService, times(1)).updateClusterMetadata(eq(stack.getId()));
        verify(clusterService, times(1)).updateClusterStatusByStackId(eq(stack.getId()), eq(DetailedStackStatus.AVAILABLE),
                eq("Services are installed but not running."));
    }

    @Test
    void testUpdateStackStatusWhenStackIsAvailableAndClusterStatusIsInstalled() {
        // GIVEN
        Stack stack = createStackWithCluster(DetailedStackStatus.NODE_FAILURE);
        when(clusterStatusService.getStatus(anyBoolean())).thenReturn(ClusterStatusResult.of(ClusterStatus.INSTALLED));
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        verify(clusterService, times(1)).updateClusterMetadata(eq(stack.getId()));
        verify(clusterService, times(1)).updateClusterStatusByStackId(eq(stack.getId()), eq(DetailedStackStatus.AVAILABLE),
                eq("Services are installed but not running."));
    }

    @Test
    void testUpdateClusterStatusShouldOnlyNotifyWhenStackStatusIsInDeletionPhase() {
        // GIVEN
        Stack stack = createStackWithCluster(DetailedStackStatus.DELETE_IN_PROGRESS);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        verify(clusterService, times(0)).updateClusterStatusByStackId(any(Long.class), any(DetailedStackStatus.class));
        verify(cloudbreakEventService, times(1))
                .fireCloudbreakEvent(eq(TEST_STACK_ID), eq(Status.DELETE_IN_PROGRESS.name()), eq(CLUSTER_AMBARI_CLUSTER_COULD_NOT_SYNC), any());
    }

    @Test
    void testUpdateClusterStatusShouldOnlyNotifyWhenStackStatusIsInStopPhase() {
        // GIVEN
        Stack stack = createStackWithCluster(DetailedStackStatus.STOPPED);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        verify(clusterService, times(0)).updateClusterStatusByStackId(any(Long.class), any(DetailedStackStatus.class));
        verify(cloudbreakEventService, times(1))
                .fireCloudbreakEvent(eq(TEST_STACK_ID), eq(Status.STOPPED.name()), eq(CLUSTER_AMBARI_CLUSTER_COULD_NOT_SYNC), any());
    }

    @Test
    void testUpdateClusterStatusShouldOnlyNotifyWhenStackStatusIsInModificationPhase() {
        // GIVEN
        Stack stack = createStackWithCluster(DetailedStackStatus.UPSCALE_IN_PROGRESS);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        verify(clusterService, times(0)).updateClusterStatusByStackId(any(Long.class), any(DetailedStackStatus.class));
        verify(cloudbreakEventService, times(1))
                .fireCloudbreakEvent(eq(TEST_STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()), eq(CLUSTER_AMBARI_CLUSTER_COULD_NOT_SYNC), any());
    }

    private Stack createStack(DetailedStackStatus detailedStackStatus) {
        Stack stack = new Stack();
        stack.setId(TEST_STACK_ID);
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus.getStatus(), "", detailedStackStatus));
        return stack;
    }

    private Stack createStackWithCluster(DetailedStackStatus detailedStackStatus) {
        Stack stack = createStack(detailedStackStatus);
        Cluster cluster = new Cluster();
        cluster.setClusterManagerIp("10.0.0.1");
        cluster.setId(TEST_CLUSTER_ID);
        Blueprint blueprint = new Blueprint();
        blueprint.setStackName(TEST_BLUEPRINT);
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        return stack;
    }
}
