package com.sequenceiq.periscope.monitor.evaluator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.repository.MetricAlertRepository;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;

public class MetricEvaluatorTest {

    private static final long CLUSTER_ID = 1L;

    @Mock
    private ClusterService clusterService;

    @Mock
    private MetricAlertRepository alertRepository;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private MetricCondition metricCondition;

    @InjectMocks
    private MetricEvaluator underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        underTest.setContext(new ClusterIdEvaluatorContext(CLUSTER_ID));
    }

    @Test
    public void testRunCallsFinished() {
        when(clusterService.findById(anyLong())).thenThrow(new RuntimeException("exception from the test"));

        underTest.run();

        verify(executorServiceWithRegistry).finished(underTest, CLUSTER_ID);
        verify(eventPublisher).publishEvent(any(UpdateFailedEvent.class));
    }

    @Test
    public void testMetricConditionReached() {
        when(clusterService.findById(anyLong())).thenReturn(createCluster());
        AmbariClient ambariClient = new AmbariClient();
        when(ambariClientProvider.createAmbariClient(any(Cluster.class))).thenReturn(ambariClient);
        MetricAlert metricAlert1 = new MetricAlert();
        MetricAlert metricAlert2 = new MetricAlert();
        when(alertRepository.findAllWithScalingPolicyByCluster(CLUSTER_ID)).thenReturn(List.of(metricAlert1, metricAlert2));
        when(metricCondition.isMetricAlertTriggered(ambariClient, metricAlert1)).thenReturn(false);
        when(metricCondition.isMetricAlertTriggered(ambariClient, metricAlert2)).thenReturn(true);

        underTest.execute();

        verify(eventPublisher, times(1)).publishEvent(any(ScalingEvent.class));
    }

    @Test
    public void testMetricConditionNotReached() {
        when(clusterService.findById(anyLong())).thenReturn(createCluster());
        AmbariClient ambariClient = new AmbariClient();
        when(ambariClientProvider.createAmbariClient(any(Cluster.class))).thenReturn(ambariClient);
        MetricAlert metricAlert1 = new MetricAlert();
        MetricAlert metricAlert2 = new MetricAlert();
        when(alertRepository.findAllWithScalingPolicyByCluster(CLUSTER_ID)).thenReturn(List.of(metricAlert1, metricAlert2));
        when(metricCondition.isMetricAlertTriggered(ambariClient, metricAlert1)).thenReturn(false);
        when(metricCondition.isMetricAlertTriggered(ambariClient, metricAlert2)).thenReturn(false);

        underTest.execute();

        verify(eventPublisher, never()).publishEvent(any(ScalingEvent.class));
    }

    private Cluster createCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setStackId(CLUSTER_ID);
        cluster.setUser(new PeriscopeUser());
        return cluster;
    }
}
