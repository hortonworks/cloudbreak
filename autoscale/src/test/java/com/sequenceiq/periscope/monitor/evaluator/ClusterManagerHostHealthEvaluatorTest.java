package com.sequenceiq.periscope.monitor.evaluator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.ChangedNodesReportV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.periscope.domain.FailedNode;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.repository.FailedNodeRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;
import com.sequenceiq.periscope.service.evaluator.HostHealthEvaluatorService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterManagerHostHealthEvaluatorTest {

    private static final long CLUSTER_ID = 1;

    private static final String STACK_CRN = "STACK_CRN";

    private static final String NEW_HEALTHY = "NEW_HEALTHY";

    private static final String NEW_FAILED = "NEW_FAILED";

    private static final String OLD_FAILED = "OLD_FAILED";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private ClusterService clusterService;

    @Mock
    private HostHealthEvaluatorService hostHealthEvaluatorService;

    @Mock
    private ClusterManagerSpecificHostHealthEvaluator clusterManagerSpecificHostHealthEvaluator;

    @Mock
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    @Mock
    private FailedNodeRepository failedNodeRepository;

    @InjectMocks
    private ClusterManagerHostHealthEvaluator underTest;

    @Mock
    private EvaluatorContext evaluatorContext;

    @Mock
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Mock
    private CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints;

    @Mock
    private AutoscaleV4Endpoint autoscaleV4Endpoint;

    @Captor
    private ArgumentCaptor<ChangedNodesReportV4Request> captor;

    @Before
    public void setUp() {
        when(evaluatorContext.getData()).thenReturn(CLUSTER_ID);
        when(hostHealthEvaluatorService.get(ClusterManagerVariant.CLOUDERA_MANAGER)).thenReturn(clusterManagerSpecificHostHealthEvaluator);

        underTest.setContext(evaluatorContext);

        when(cloudbreakClientConfiguration.cloudbreakInternalCrnClientClient()).thenReturn(cloudbreakInternalCrnClient);
        when(cloudbreakInternalCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakServiceCrnEndpoints.autoscaleEndpoint()).thenReturn(autoscaleV4Endpoint);
    }

    @Test
    public void shouldReportNewFailedAndHealthyNodes() {
        Cluster cluster = getCluster();
        when(clusterService.findById(CLUSTER_ID)).thenReturn(cluster);
        FailedNode oldFailedNode = getFailedNode(OLD_FAILED);
        FailedNode newHealthyNode = getFailedNode(NEW_HEALTHY);
        when(failedNodeRepository.findByClusterId(CLUSTER_ID)).thenReturn(List.of(oldFailedNode, newHealthyNode));
        when(clusterManagerSpecificHostHealthEvaluator.determineHostnamesToRecover(cluster)).thenReturn(List.of(OLD_FAILED, NEW_FAILED));

        underTest.execute();

        verify(autoscaleV4Endpoint).changedNodesReport(eq(STACK_CRN), captor.capture());
        FailedNode newFailedNode = new FailedNode();
        newFailedNode.setClusterId(CLUSTER_ID);
        newFailedNode.setName(NEW_FAILED);
        verify(failedNodeRepository).findByClusterId(CLUSTER_ID);
        verify(failedNodeRepository).saveAll(List.of(newFailedNode));
        verify(failedNodeRepository).deleteAll(List.of(newHealthyNode));
        verifyNoMoreInteractions(failedNodeRepository);

        ChangedNodesReportV4Request request = captor.getValue();
        assertThat(request.getNewFailedNodes(), is(List.of(NEW_FAILED)));
        assertThat(request.getNewHealthyNodes(), is(List.of(NEW_HEALTHY)));
    }

    @Test
    public void shouldNotUpdateFailedNodesIfErrorHappens() {
        Cluster cluster = getCluster();
        when(clusterService.findById(CLUSTER_ID)).thenReturn(cluster);
        when(clusterManagerSpecificHostHealthEvaluator.determineHostnamesToRecover(cluster)).thenReturn(List.of(NEW_FAILED));
        doThrow(new RuntimeException("API exception")).when(autoscaleV4Endpoint).changedNodesReport(anyString(), ArgumentMatchers.any());

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("API exception");

        underTest.execute();

        verify(failedNodeRepository).findByClusterId(CLUSTER_ID);
        verifyNoMoreInteractions(failedNodeRepository);
    }

    @Test
    public void shouldNotUpdateWhenFailedNodesNotChanged() {
        Cluster cluster = getCluster();
        when(clusterService.findById(CLUSTER_ID)).thenReturn(cluster);
        FailedNode failedNode = getFailedNode(OLD_FAILED);
        when(failedNodeRepository.findByClusterId(CLUSTER_ID)).thenReturn(List.of(failedNode));
        when(clusterManagerSpecificHostHealthEvaluator.determineHostnamesToRecover(cluster)).thenReturn(List.of(OLD_FAILED));

        underTest.execute();

        verifyZeroInteractions(cloudbreakInternalCrnClient);
        verify(failedNodeRepository).findByClusterId(CLUSTER_ID);
        verifyNoMoreInteractions(failedNodeRepository);
    }

    private Cluster getCluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(STACK_CRN);
        ClusterManager cm = new ClusterManager();
        cm.setVariant(ClusterManagerVariant.CLOUDERA_MANAGER);
        cluster.setClusterManager(cm);
        return cluster;
    }

    private FailedNode getFailedNode(String name) {
        FailedNode failedNode = new FailedNode();
        failedNode.setClusterId(CLUSTER_ID);
        failedNode.setName(name);
        return failedNode;
    }
}