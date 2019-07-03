package com.sequenceiq.periscope.monitor.evaluator;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.periscope.monitor.context.ClusterCreationEvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.ambari.AmbariClusterCreationEvaluator;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

public class AmbariClusterCreationEvaluatorTest {

    public static final long STACK_ID = 1L;

    @Mock
    private ClusterService clusterService;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private HistoryService historyService;

    @Mock
    private HttpNotificationSender notificationSender;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @InjectMocks
    private AmbariClusterCreationEvaluator underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRunCallsFinished() {
        AutoscaleStackV4Response response = new AutoscaleStackV4Response();
        response.setStackId(STACK_ID);
        underTest.setContext(new ClusterCreationEvaluatorContext(response));
        when(clusterService.findOneByStackId(STACK_ID)).thenThrow(new RuntimeException("exception from the test"));

        underTest.run();

        verify(executorServiceWithRegistry).finished(underTest, STACK_ID);
    }
}
