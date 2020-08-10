package com.sequenceiq.periscope.monitor.evaluator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.FailedNode;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.repository.FailedNodeRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.evaluator.HostHealthEvaluatorService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterManagerHostHealthEvaluatorTest {

    private static final long CLUSTER_ID = 1;

    private static final String STACK_CRN = "STACK_CRN";

    private static final String NEW_HEALTHY = "NEW_HEALTHY";

    private static final String NEW_FAILED = "NEW_FAILED";

    private static final String OLD_FAILED = "OLD_FAILED";

    @Mock
    private ClusterService clusterService;

    @Mock
    private HostHealthEvaluatorService hostHealthEvaluatorService;

    @Mock
    private ClusterManagerSpecificHostHealthEvaluator clusterManagerSpecificHostHealthEvaluator;

    @Mock
    private FailedNodeRepository failedNodeRepository;

    @InjectMocks
    private ClusterManagerHostHealthEvaluator underTest;

    @Mock
    private EvaluatorContext evaluatorContext;

    @Captor
    private ArgumentCaptor<List<FailedNode>> captorSave;

    @Captor
    private ArgumentCaptor<List<FailedNode>> captorDelete;

    @Before
    public void setUp() {
        when(evaluatorContext.getData()).thenReturn(CLUSTER_ID);
        when(hostHealthEvaluatorService.get(ClusterManagerVariant.CLOUDERA_MANAGER)).thenReturn(clusterManagerSpecificHostHealthEvaluator);

        underTest.setContext(evaluatorContext);
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

        verify(failedNodeRepository).saveAll(captorSave.capture());
        verify(failedNodeRepository).deleteAll(captorDelete.capture());
        FailedNode newFailedNode = new FailedNode();
        newFailedNode.setClusterId(CLUSTER_ID);
        newFailedNode.setName(NEW_FAILED);
        verify(failedNodeRepository).findByClusterId(CLUSTER_ID);
        verify(failedNodeRepository).saveAll(List.of(newFailedNode));
        verify(failedNodeRepository).deleteAll(List.of(newHealthyNode));
        verifyNoMoreInteractions(failedNodeRepository);

        List<FailedNode> saveRequest = captorSave.getValue();
        assertThat(saveRequest.stream().map(FailedNode::getName).collect(Collectors.toList()), is(List.of(NEW_FAILED)));
        List<FailedNode> deleteRequest = captorDelete.getValue();
        assertThat(deleteRequest.stream().map(FailedNode::getName).collect(Collectors.toList()), is(List.of(NEW_HEALTHY)));
    }

    @Test
    public void shouldNotUpdateWhenFailedNodesNotChanged() {
        Cluster cluster = getCluster();
        when(clusterService.findById(CLUSTER_ID)).thenReturn(cluster);
        FailedNode failedNode = getFailedNode(OLD_FAILED);
        when(failedNodeRepository.findByClusterId(CLUSTER_ID)).thenReturn(List.of(failedNode));
        when(clusterManagerSpecificHostHealthEvaluator.determineHostnamesToRecover(cluster)).thenReturn(List.of(OLD_FAILED));

        underTest.execute();

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