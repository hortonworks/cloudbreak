package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

public class AmbariClusterStatusUpdaterTest {
    private static final Long TEST_STACK_ID = 0L;

    private static final Long TEST_CLUSTER_ID = 0L;

    private static final String TEST_BLUEPRINT = "blueprint";

    private static final String TEST_REASON = "Reason";

    @InjectMocks
    private AmbariClusterStatusUpdater underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private AmbariClusterStatusFactory clusterStatusFactory;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Before
    public void setUp() {
        underTest = new AmbariClusterStatusUpdater();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateStackStatusWhenStackStatusChanged() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.AVAILABLE);
        BDDMockito.given(ambariClientProvider.getAmbariClient(ArgumentMatchers.nullable(HttpClientConfig.class),
                ArgumentMatchers.nullable(Integer.class), any(Cluster.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactory.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(ClusterStatus.INSTALLED);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        Mockito.verify(clusterService, Mockito.times(1)).updateClusterStatusByStackId(stack.getId(), Status.STOPPED);
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateStackStatusWhenThereAreStoppedServices() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.UPDATE_FAILED);
        BDDMockito.given(ambariClientProvider.getAmbariClient(ArgumentMatchers.nullable(HttpClientConfig.class),
                ArgumentMatchers.nullable(Integer.class), any(Cluster.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactory.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(ClusterStatus.AMBIGUOUS);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        Mockito.verify(clusterService, Mockito.times(1)).updateClusterStatusByStackId(stack.getId(), Status.AVAILABLE);
    }

    @Test
    public void testUpdateClusterStatusShouldNotUpdateStackStatusWhenMaintenanceModeIsEnabledAndCLusterIsAvailable() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.MAINTENANCE_MODE_ENABLED);
        BDDMockito.given(ambariClientProvider.getAmbariClient(ArgumentMatchers.nullable(HttpClientConfig.class),
                ArgumentMatchers.nullable(Integer.class), any(Cluster.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactory.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(ClusterStatus.STARTED);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        Mockito.verify(clusterService, Mockito.never()).updateClusterStatusByStackId(eq(stack.getId()), any(Status.class));
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateStackStatusWhenMaintenanceModeIsEnabledButClusterIsStopped() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.MAINTENANCE_MODE_ENABLED);
        BDDMockito.given(ambariClientProvider.getAmbariClient(ArgumentMatchers.nullable(HttpClientConfig.class),
                ArgumentMatchers.nullable(Integer.class), any(Cluster.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactory.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(ClusterStatus.INSTALLED);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        Mockito.verify(clusterService, Mockito.times(1)).updateClusterStatusByStackId(stack.getId(), Status.STOPPED);
    }

    @Test
    public void testUpdateClusterStatusShouldOnlyNotifyWhenStackStatusNotChanged() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.AVAILABLE);
        BDDMockito.given(ambariClientProvider.getAmbariClient(ArgumentMatchers.nullable(HttpClientConfig.class),
                ArgumentMatchers.nullable(Integer.class), any(Cluster.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactory.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(ClusterStatus.STARTED);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        Mockito.verify(clusterService, Mockito.times(0)).updateClusterStatusByStackId(any(Long.class), any(Status.class));
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateWhenStackStatusStopped() {
        // GIVEN
        Stack stack = createStack(Status.STOPPED, Status.AVAILABLE);
        BDDMockito.given(ambariClientProvider.getAmbariClient(BDDMockito.nullable(HttpClientConfig.class),
                BDDMockito.nullable(Integer.class), any(Cluster.class))).willThrow(new RuntimeException("ex"));
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
        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setStackName(TEST_BLUEPRINT);
        cluster.setClusterDefinition(clusterDefinition);
        stack.setCluster(cluster);
        return stack;
    }
}
