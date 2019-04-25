package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

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
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;

public class ClusterStatusUpdaterTest {
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

    @Before
    public void setUp() {
        underTest = new ClusterStatusUpdater();
        MockitoAnnotations.initMocks(this);
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.getStatus(anyBoolean())).thenReturn(ClusterStatusResult.of(ClusterStatus.STARTED));
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateStackStatusWhenStackStatusChanged() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.AVAILABLE);
        when(clusterStatusService.getStatus(anyBoolean())).thenReturn(ClusterStatusResult.of(ClusterStatus.INSTALLED));
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        Mockito.verify(clusterService, Mockito.times(1)).updateClusterStatusByStackId(stack.getId(), Status.STOPPED);
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateStackStatusWhenThereAreStoppedServices() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.UPDATE_FAILED);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        Mockito.verify(clusterService, Mockito.times(1)).updateClusterStatusByStackId(stack.getId(), Status.AVAILABLE);
    }

    @Test
    public void testUpdateClusterStatusShouldNotUpdateStackStatusWhenMaintenanceModeIsEnabledAndCLusterIsAvailable() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.MAINTENANCE_MODE_ENABLED);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        Mockito.verify(clusterService, Mockito.never()).updateClusterStatusByStackId(eq(stack.getId()), any(Status.class));
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateStackStatusWhenMaintenanceModeIsEnabledButClusterIsStopped() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.MAINTENANCE_MODE_ENABLED);
        when(clusterStatusService.getStatus(anyBoolean())).thenReturn(ClusterStatusResult.of(ClusterStatus.INSTALLED));
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        Mockito.verify(clusterService, Mockito.times(1)).updateClusterStatusByStackId(stack.getId(), Status.STOPPED);
    }

    @Test
    public void testUpdateClusterStatusShouldOnlyNotifyWhenStackStatusNotChanged() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.AVAILABLE);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        Mockito.verify(clusterService, Mockito.times(0)).updateClusterStatusByStackId(any(Long.class), any(Status.class));
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateWhenStackStatusStopped() {
        // GIVEN
        Stack stack = createStack(Status.STOPPED, Status.AVAILABLE);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        BDDMockito.verify(clusterService, BDDMockito.times(1)).updateClusterStatusByStackId(any(Long.class), eq(stack.getStatus()));
    }

    private Stack createStack(Status stackStatus) {
        Stack stack = new Stack();
        stack.setId(TEST_STACK_ID);
        stack.setStackStatus(new StackStatus(stack, stackStatus, "", DetailedStackStatus.UNKNOWN));
        return stack;
    }

    private Stack createStack(Status stackStatus, Status clusterStatus) {
        Stack stack = createStack(stackStatus);
        Cluster cluster = new Cluster();
        cluster.setAmbariIp("10.0.0.1");
        cluster.setId(TEST_CLUSTER_ID);
        cluster.setStatus(clusterStatus);
        Blueprint blueprint = new Blueprint();
        blueprint.setStackName(TEST_BLUEPRINT);
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        return stack;
    }
}
