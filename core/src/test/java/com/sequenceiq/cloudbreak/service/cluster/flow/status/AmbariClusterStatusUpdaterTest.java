package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
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
    public void testUpdateClusterStatusShouldUpdateStackStatusWhenStackStatusChanged() throws CloudbreakSecuritySetupException {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.AVAILABLE);
        BDDMockito.given(ambariClientProvider.getAmbariClient(BDDMockito.nullable(HttpClientConfig.class),
                BDDMockito.nullable(Integer.class), BDDMockito.any(Cluster.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactory.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(ClusterStatus.INSTALLED);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        BDDMockito.verify(clusterService, BDDMockito.times(1)).updateClusterStatusByStackId(stack.getId(), Status.STOPPED);
    }

    @Test
    public void testUpdateClusterStatusShouldOnlyNotifyWhenStackStatusNotChanged() throws CloudbreakSecuritySetupException {
        // GIVEN
        Stack stack = createStack(Status.AVAILABLE, Status.AVAILABLE);
        BDDMockito.given(ambariClientProvider.getAmbariClient(BDDMockito.nullable(HttpClientConfig.class),
                BDDMockito.nullable(Integer.class), BDDMockito.any(Cluster.class))).willReturn(ambariClient);
        BDDMockito.given(clusterStatusFactory.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(ClusterStatus.STARTED);
        // WHEN
        underTest.updateClusterStatus(stack, stack.getCluster());
        // THEN
        BDDMockito.verify(clusterService, BDDMockito.times(0)).updateClusterStatusByStackId(BDDMockito.any(Long.class), BDDMockito.any(Status.class));
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
        blueprint.setAmbariName(TEST_BLUEPRINT);
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        return stack;
    }
}
