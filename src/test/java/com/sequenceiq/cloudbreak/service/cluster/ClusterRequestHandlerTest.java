package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterRequestHandler;
import com.sequenceiq.cloudbreak.service.stack.connector.aws.AwsConnectorTestUtil;

import reactor.event.Event;

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
        User user = AwsConnectorTestUtil.createUser();
        AwsCredential credential = AwsConnectorTestUtil.createAwsCredential();
        AwsTemplate awsTemplate = AwsConnectorTestUtil.createAwsTemplate();
        stackEvent = new Event<>(AwsConnectorTestUtil.createStack(AwsConnectorTestUtil.DUMMY_OWNER, AwsConnectorTestUtil.DUMMY_ACCOUNT, credential, awsTemplate, new HashSet<Resource>()));
        stackEvent.setKey(AMBARI_STARTED);
    }

    @Test
    public void testAcceptEventWhenAmbariStartedAndStatusRequestedShouldInstallAmbariCluster() {
        // GIVEN
        stackEvent.getData().setCluster(createClusterWithStatus(Status.REQUESTED));
        doNothing().when(ambariClusterInstaller).installAmbariCluster(stackEvent.getData());
        // WHEN
        underTest.accept(stackEvent);
        // THEN
        verify(ambariClusterInstaller, times(1)).installAmbariCluster(any(Stack.class));
    }

    @Test
    public void testAcceptEventWhenAmbariStartedAndStatusNotRequestedShouldNotInstallAmbariCluster() {
        // GIVEN
        stackEvent.getData().setCluster(createClusterWithStatus(Status.CREATE_IN_PROGRESS));
        // WHEN
        underTest.accept(stackEvent);
        // THEN
        verify(ambariClusterInstaller, times(0)).installAmbariCluster(any(Stack.class));
    }

    @Test
    public void testAcceptEventWhenClusterRequestedAndStackStatusIsCreateCompletedShouldInstallAmbariCluster() {
        // GIVEN
        stackEvent.setKey(CLUSTER_REQUESTED);
        stackEvent.getData().setCluster(createClusterWithStatus(Status.REQUESTED));
        stackEvent.getData().setStatus(Status.AVAILABLE);
        doNothing().when(ambariClusterInstaller).installAmbariCluster(stackEvent.getData());
        // WHEN
        underTest.accept(stackEvent);
        // THEN
        verify(ambariClusterInstaller, times(1)).installAmbariCluster(any(Stack.class));
    }

    @Test
    public void testAcceptEventWhenClusterRequestedAndStackStatusIsNotCreateCompletedShouldNotInstallAmbariCluster() {
        // GIVEN
        stackEvent.setKey(CLUSTER_REQUESTED);
        stackEvent.getData().setCluster(createClusterWithStatus(Status.REQUESTED));
        stackEvent.getData().setStatus(Status.CREATE_IN_PROGRESS);
        doNothing().when(ambariClusterInstaller).installAmbariCluster(stackEvent.getData());
        // WHEN
        underTest.accept(stackEvent);
        // THEN
        verify(ambariClusterInstaller, times(0)).installAmbariCluster(any(Stack.class));
    }

    private Cluster createClusterWithStatus(Status status) {
        Cluster cluster = new Cluster();
        cluster.setStatus(status);
        return cluster;
    }
}
