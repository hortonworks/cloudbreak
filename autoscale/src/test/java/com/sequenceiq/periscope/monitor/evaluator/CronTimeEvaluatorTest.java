package com.sequenceiq.periscope.monitor.evaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.service.ClusterService;

public class CronTimeEvaluatorTest {

    private static final long CLUSTER_ID = 1L;

    @Mock
    private TimeAlertRepository alertRepository;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @InjectMocks
    private CronTimeEvaluator underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRunCallsFinished() {
        underTest.setContext(new ClusterIdEvaluatorContext(CLUSTER_ID));
        when(clusterService.find(anyLong())).thenThrow(new RuntimeException("exception from the test"));

        try {
            underTest.run();
            fail("expected runtimeException");
        } catch (RuntimeException e) {
            assertEquals("exception from the test", e.getMessage());
        }

        verify(executorServiceWithRegistry).finished(underTest, CLUSTER_ID);
    }
}
