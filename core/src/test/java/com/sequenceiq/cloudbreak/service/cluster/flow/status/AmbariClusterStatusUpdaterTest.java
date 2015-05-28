package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import static org.mockito.Matchers.any;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

public class AmbariClusterStatusUpdaterTest {
    private static final Long TEST_STACK_ID = 0L;
    private static final Long TEST_CLUSTER_ID = 0L;
    private static final String TEST_BLUEPRINT = "blueprint";
    private static final String TEST_REASON = "Reason";

    @InjectMocks
    private AmbariClusterStatusUpdater underTest;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private RetryingStackUpdater stackUpdater;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private AmbariClusterStatusFactory clusterStatusFactoy;

    @Mock
    private AmbariClient ambariClient;

    @Before
    public void setUp() {
        underTest = new AmbariClusterStatusUpdater();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdateClusterStatusShouldDoNothingWhenStatusCheckIsNotNecessary() {
        // GIVEN
        Stack stack = createStack(Status.STOP_IN_PROGRESS, Status.STOP_REQUESTED);
        // WHEN
        underTest.updateClusterStatus(stack);
        // THEN
        Assert.assertEquals(Status.STOP_IN_PROGRESS, stack.getStatus());
        Assert.assertEquals(Status.STOP_REQUESTED, stack.getCluster().getStatus());
        BDDMockito.verify(clusterRepository, BDDMockito.times(0)).save(BDDMockito.any(Cluster.class));
        BDDMockito.verify(stackUpdater, BDDMockito.times(0)).updateStackStatus(BDDMockito.any(Long.class), BDDMockito.any(Status.class),
                BDDMockito.any(String.class));
        BDDMockito.verify(cloudbreakEventService, BDDMockito.times(0)).fireCloudbreakEvent(BDDMockito.any(Long.class), BDDMockito.any(String.class),
                BDDMockito.any(String.class));
    }

    @Test
    public void testUpdateClusterStatusShouldDoNothingWhenNoClusterGivenInStack() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE);
        // WHEN
        underTest.updateClusterStatus(stack);
        // THEN
        Assert.assertEquals(Status.AVAILABLE, stack.getStatus());
        Assert.assertNull(stack.getCluster());
        BDDMockito.verify(clusterRepository, BDDMockito.times(0)).save(BDDMockito.any(Cluster.class));
        BDDMockito.verify(stackUpdater, BDDMockito.times(0)).updateStackStatus(BDDMockito.any(Long.class), BDDMockito.any(Status.class),
                BDDMockito.any(String.class));
        BDDMockito.verify(cloudbreakEventService, BDDMockito.times(0)).fireCloudbreakEvent(BDDMockito.any(Long.class), BDDMockito.any(String.class),
                BDDMockito.any(String.class));
    }

    @Test
    public void testUpdateClusterStatusShouldDoNothingWhenClusterStatusFactoryReturnsNull() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.AVAILABLE);
        BDDMockito.given(ambariClientProvider.getAmbariClient(any(String.class), any(String.class), any(String.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactoy.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(null);
        // WHEN
        underTest.updateClusterStatus(stack);
        // THEN
        Assert.assertEquals(Status.AVAILABLE, stack.getStatus());
        Assert.assertEquals(Status.AVAILABLE, stack.getCluster().getStatus());
        BDDMockito.verify(clusterRepository, BDDMockito.times(0)).save(BDDMockito.any(Cluster.class));
        BDDMockito.verify(stackUpdater, BDDMockito.times(0)).updateStackStatus(BDDMockito.any(Long.class), BDDMockito.any(Status.class),
                BDDMockito.any(String.class));
        BDDMockito.verify(cloudbreakEventService, BDDMockito.times(0)).fireCloudbreakEvent(BDDMockito.any(Long.class), BDDMockito.any(String.class),
                BDDMockito.any(String.class));
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateStackStatusWhenStackStatusChanged() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.STOPPED);
        AmbariClusterStatus clusterStatus = new AmbariClusterStatus(ClusterStatus.INSTALLED, Status.STOPPED, Status.STOPPED, TEST_REASON);
        BDDMockito.given(ambariClientProvider.getAmbariClient(any(String.class), any(String.class), any(String.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactoy.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(clusterStatus);
        // WHEN
        underTest.updateClusterStatus(stack);
        // THEN
        Assert.assertEquals(Status.STOPPED, stack.getCluster().getStatus());
        BDDMockito.verify(stackUpdater, BDDMockito.times(0)).updateStackStatus(stack.getId(), Status.STOPPED, TEST_REASON);
        BDDMockito.verify(clusterRepository, BDDMockito.times(0)).save(BDDMockito.any(Cluster.class));
        BDDMockito.verify(cloudbreakEventService, BDDMockito.times(0)).fireCloudbreakEvent(BDDMockito.any(Long.class), BDDMockito.any(String.class),
                BDDMockito.any(String.class));
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateClusterStatusWhenClusterStatusChanged() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.AVAILABLE);
        AmbariClusterStatus clusterStatus = new AmbariClusterStatus(ClusterStatus.INSTALLED, Status.AVAILABLE, Status.STOPPED, TEST_REASON);
        BDDMockito.given(ambariClientProvider.getAmbariClient(any(String.class), any(String.class), any(String.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactoy.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(clusterStatus);
        // WHEN
        underTest.updateClusterStatus(stack);
        // THEN
        Assert.assertEquals(Status.AVAILABLE, stack.getCluster().getStatus());
        BDDMockito.verify(clusterRepository, BDDMockito.times(0)).save(stack.getCluster());
        BDDMockito.verify(cloudbreakEventService, BDDMockito.times(0)).fireCloudbreakEvent(stack.getId(), Status.AVAILABLE.name(), TEST_REASON);
        BDDMockito.verify(stackUpdater, BDDMockito.times(0)).updateStackStatus(BDDMockito.any(Long.class), BDDMockito.any(Status.class),
                BDDMockito.any(String.class));
    }

    @Test
    public void testUpdateClusterStatusShouldUpdateStackAndClusterStatusWhenBothChanged() {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.AVAILABLE);
        AmbariClusterStatus clusterStatus = new AmbariClusterStatus(ClusterStatus.INSTALLED, Status.STOPPED, Status.STOPPED, TEST_REASON);
        BDDMockito.given(ambariClientProvider.getAmbariClient(any(String.class), any(String.class), any(String.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactoy.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(clusterStatus);
        // WHEN
        underTest.updateClusterStatus(stack);
        // THEN
        Assert.assertEquals(Status.AVAILABLE, stack.getCluster().getStatus());
        BDDMockito.verify(clusterRepository, BDDMockito.times(0)).save(stack.getCluster());
        BDDMockito.verify(stackUpdater, BDDMockito.times(0)).updateStackStatus(stack.getId(), Status.STOPPED, TEST_REASON);
        BDDMockito.verify(cloudbreakEventService, BDDMockito.times(0)).fireCloudbreakEvent(BDDMockito.any(Long.class), BDDMockito.any(String.class),
                BDDMockito.any(String.class));
    }

    private Stack createStack(Status stackStatus) {
        Stack stack = new Stack();
        stack.setId(TEST_STACK_ID);
        stack.setStatus(stackStatus);
        return stack;
    }

    private Stack createStack(Status stackStatus, Status clusterStatus) {
        Stack stack = createStack(stackStatus);
        Cluster cluster = new Cluster();
        cluster.setId(TEST_CLUSTER_ID);
        cluster.setStatus(clusterStatus);
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintName(TEST_BLUEPRINT);
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        return stack;
    }
}
