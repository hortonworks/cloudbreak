package com.sequenceiq.periscope.monitor.evaluator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.ambari.AmbariAgentHealthEvaluator;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

public class AmbariAgentHealthEvaluatorTest {

    private static final long CLUSTER_ID = 1L;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Mock
    private ClusterService clusterService;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private AmbariAgentHealthEvaluator underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRunCallsFinished() {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);

        when(ambariClientProvider.createAmbariClient(cluster)).thenThrow(new RuntimeException("exception from the test"));
        underTest.determineHostnamesToRecover(cluster);

        verify(eventPublisher).publishEvent(any(UpdateFailedEvent.class));
    }
}
