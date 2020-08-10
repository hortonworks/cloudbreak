package com.sequenceiq.periscope.monitor.evaluator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.repository.PrometheusAlertRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

public class PrometheusEvaluatorTest {

    private static final long CLUSTER_ID = 1L;

    @Mock
    private ClusterService clusterService;

    @Mock
    private PrometheusAlertRepository alertRepository;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PrometheusEvaluator underTest;

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
}
