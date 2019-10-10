package com.sequenceiq.periscope.monitor.evaluator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.autoscale.AutoscaleEndpoint;
import com.sequenceiq.cloudbreak.api.model.ChangedNodesReport;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.FailedNode;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.repository.FailedNodeRepository;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

public class AmbariAgentHealthEvaluatorTest {

    private static final long CLUSTER_ID = 1L;

    private static final long STACK_ID = 1L;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Mock
    private ClusterService clusterService;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    @Mock
    private AmbariRequestLogging ambariRequestLogging;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private FailedNodeRepository failedNodeRepository;

    @InjectMocks
    private AmbariAgentHealthEvaluator underTest;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private AutoscaleEndpoint autoscaleEndpoint;

    @Mock
    private Response response;

    @Captor
    private ArgumentCaptor<ChangedNodesReport> captor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRunCallsFinished() {
        underTest.setContext(new ClusterIdEvaluatorContext(CLUSTER_ID));
        when(clusterService.findById(anyLong())).thenThrow(new RuntimeException("exception from the test"));

        underTest.run();

        verify(executorServiceWithRegistry).finished(underTest, CLUSTER_ID);
        verify(eventPublisher).publishEvent(any(UpdateFailedEvent.class));
    }

    @Test
    public void shouldReportNewFailedAndNewHealthyNodes() {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setUser(new PeriscopeUser());
        cluster.setStackId(STACK_ID);
        when(clusterService.findById(CLUSTER_ID)).thenReturn(cluster);
        when(ambariClientProvider.createAmbariClient(any())).thenReturn(ambariClient);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(ambariRequestLogging).logging(any(), anyString());
        when(cloudbreakClientConfiguration.cloudbreakClient()).thenReturn(cloudbreakClient);
        when(cloudbreakClient.autoscaleEndpoint()).thenReturn(autoscaleEndpoint);
        when(ambariClient.getAlertByNameAndState("ambari_server_agent_heartbeat", "CRITICAL"))
                .thenReturn(List.of(Map.of("state", "CRITICAL", "host_name", "host_1")));
        when(autoscaleEndpoint.changedNodesReport(anyLong(), any())).thenReturn(response);
        when(response.getStatus()).thenReturn(Status.ACCEPTED.getStatusCode());

        FailedNode previouslyFailedNode = new FailedNode();
        previouslyFailedNode.setClusterId(CLUSTER_ID);
        previouslyFailedNode.setName("host_2");
        when(failedNodeRepository.findByClusterId(CLUSTER_ID)).thenReturn(List.of(previouslyFailedNode));

        underTest.setContext(new ClusterIdEvaluatorContext(CLUSTER_ID));
        underTest.execute();

        InOrder inOrder = Mockito.inOrder(autoscaleEndpoint, failedNodeRepository);
        inOrder.verify(autoscaleEndpoint).changedNodesReport(eq(STACK_ID), captor.capture());
        ChangedNodesReport changedNodesReport = captor.getValue();
        assertThat(changedNodesReport.getNewFailedNodes(), hasItems("host_1"));
        assertThat(changedNodesReport.getNewHealthyNodes(), hasItems("host_2"));
        FailedNode newFailedNode = new FailedNode();
        newFailedNode.setClusterId(CLUSTER_ID);
        newFailedNode.setName("host_1");
        inOrder.verify(failedNodeRepository).saveAll(List.of(newFailedNode));
        inOrder.verify(failedNodeRepository).deleteAll(List.of(previouslyFailedNode));
    }

    @Test
    public void shouldNotSaveNewFailedNodeIfCloudbreakCommunicationFails() {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setUser(new PeriscopeUser());
        cluster.setStackId(STACK_ID);
        when(clusterService.findById(CLUSTER_ID)).thenReturn(cluster);
        when(ambariClientProvider.createAmbariClient(any())).thenReturn(ambariClient);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(ambariRequestLogging).logging(any(), anyString());
        when(cloudbreakClientConfiguration.cloudbreakClient()).thenReturn(cloudbreakClient);
        when(cloudbreakClient.autoscaleEndpoint()).thenReturn(autoscaleEndpoint);
        when(ambariClient.getAlertByNameAndState("ambari_server_agent_heartbeat", "CRITICAL"))
                .thenReturn(List.of(Map.of("state", "CRITICAL", "host_name", "host_1")));
        when(autoscaleEndpoint.changedNodesReport(anyLong(), any())).thenReturn(response);
        when(response.getStatus()).thenReturn(Status.BAD_REQUEST.getStatusCode());

        FailedNode previouslyFailedNode = new FailedNode();
        previouslyFailedNode.setClusterId(CLUSTER_ID);
        previouslyFailedNode.setName("host_2");
        when(failedNodeRepository.findByClusterId(CLUSTER_ID)).thenReturn(List.of(previouslyFailedNode));

        underTest.setContext(new ClusterIdEvaluatorContext(CLUSTER_ID));
        underTest.execute();

        verify(autoscaleEndpoint).changedNodesReport(eq(STACK_ID), captor.capture());
        ChangedNodesReport changedNodesReport = captor.getValue();
        assertThat(changedNodesReport.getNewFailedNodes(), hasItems("host_1"));
        assertThat(changedNodesReport.getNewHealthyNodes(), hasItems("host_2"));
        verify(failedNodeRepository).findByClusterId(CLUSTER_ID);
        verifyNoMoreInteractions(failedNodeRepository);
    }
}
