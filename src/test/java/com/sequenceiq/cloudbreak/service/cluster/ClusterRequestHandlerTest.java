package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterRequestHandler;
import com.sequenceiq.cloudbreak.service.stack.connector.aws.AwsConnectorTestUtil;

import reactor.event.Event;

@Ignore("Rewrite test!")
public class ClusterRequestHandlerTest {
    public static final String CLUSTER_REQUESTED = "CLUSTER_REQUESTED";
    public static final String AMBARI_STARTED = "AMBARI_STARTED";

    @InjectMocks
    private ClusterRequestHandler underTest;

    @Mock
    private AmbariClusterConnector ambariClusterInstaller;

    private Event<Stack> stackEvent;

    @Before
    public void setUp() {
        underTest = new ClusterRequestHandler();
        MockitoAnnotations.initMocks(this);
        Credential credential = AwsConnectorTestUtil.createAwsCredential();
        Template awsTemplate = AwsConnectorTestUtil.createAwsTemplate();
        stackEvent = new Event<>(ServiceTestUtils.createStack(awsTemplate, credential));
        stackEvent.setKey(AMBARI_STARTED);
    }

    @Test
    public void testAcceptEventWhenAmbariStartedAndStatusRequestedShouldInstallAmbariCluster() throws Exception {
        // GIVEN
        stackEvent.getData().setCluster(createClusterWithStatus(Status.REQUESTED));
        doNothing().when(ambariClusterInstaller).buildAmbariCluster(stackEvent.getData());
        // WHEN
        underTest.accept(stackEvent);
        // THEN
        verify(ambariClusterInstaller, times(1)).buildAmbariCluster(any(Stack.class));
    }

    @Test
    public void testAcceptEventWhenAmbariStartedAndStatusNotRequestedShouldNotInstallAmbariCluster() throws Exception {
        // GIVEN
        stackEvent.getData().setCluster(createClusterWithStatus(Status.CREATE_IN_PROGRESS));
        // WHEN
        underTest.accept(stackEvent);
        // THEN
        verify(ambariClusterInstaller, times(0)).buildAmbariCluster(any(Stack.class));
    }

    @Test
    public void testAcceptEventWhenClusterRequestedAndStackStatusIsCreateCompletedShouldInstallAmbariCluster() throws Exception {
        // GIVEN
        stackEvent.setKey(CLUSTER_REQUESTED);
        stackEvent.getData().setCluster(createClusterWithStatus(Status.REQUESTED));
        stackEvent.getData().setStatus(Status.AVAILABLE);
        doNothing().when(ambariClusterInstaller).buildAmbariCluster(stackEvent.getData());
        // WHEN
        underTest.accept(stackEvent);
        // THEN
        verify(ambariClusterInstaller, times(1)).buildAmbariCluster(any(Stack.class));
    }

    @Test
    public void testAcceptEventWhenClusterRequestedAndStackStatusIsNotCreateCompletedShouldNotInstallAmbariCluster() throws Exception {
        // GIVEN
        stackEvent.setKey(CLUSTER_REQUESTED);
        stackEvent.getData().setCluster(createClusterWithStatus(Status.REQUESTED));
        stackEvent.getData().setStatus(Status.CREATE_IN_PROGRESS);
        doNothing().when(ambariClusterInstaller).buildAmbariCluster(stackEvent.getData());
        // WHEN
        underTest.accept(stackEvent);
        // THEN
        verify(ambariClusterInstaller, times(0)).buildAmbariCluster(any(Stack.class));
    }

    private Cluster createClusterWithStatus(Status status) {
        Cluster cluster = new Cluster();
        cluster.setStatus(status);
        return cluster;
    }
}
