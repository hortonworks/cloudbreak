package com.sequenceiq.periscope.monitor;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;

public class AbstractMonitorTest {

    private static final String EXECUTOR_NAME = "executorName";

    private static final long CLUSTER_ID = 1L;

    @Mock
    private PeriscopeNodeConfig periscopeNodeConfig;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Mock
    private RejectedThreadService rejectedThreadService;

    @Mock
    private Monitored monitored;

    private TestExecutor testExecutor = new TestExecutor();

    private AbstractMonitor underTest = getMonitor();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecute() {
        List<Cluster> clusterList = getClusters();
        when(periscopeNodeConfig.getId()).thenReturn("nodeId");
        when(clusterService.findAllForNode(ClusterState.RUNNING, true, "nodeId")).thenReturn(clusterList);

        underTest.execute(getContext());

        verify(executorServiceWithRegistry).submitIfAbsent(testExecutor, CLUSTER_ID);
        verify(monitored).setLastEvaluated(anyLong());
        verify(rejectedThreadService).remove(CLUSTER_ID);
    }

    private JobExecutionContext getContext() {
        JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap jobDataMap = mock(JobDataMap.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(jobDataMap.get("APPLICATION_CONTEXT")).thenReturn(applicationContext);
        when(applicationContext.getBean(ExecutorServiceWithRegistry.class)).thenReturn(executorServiceWithRegistry);
        when(applicationContext.getBean(ClusterService.class)).thenReturn(clusterService);
        when(applicationContext.getBean(PeriscopeNodeConfig.class)).thenReturn(periscopeNodeConfig);
        when(applicationContext.getBean(testExecutor.getClass().getSimpleName(), EvaluatorExecutor.class)).thenReturn(testExecutor);
        when(applicationContext.getBean(RejectedThreadService.class)).thenReturn(rejectedThreadService);

        return jobExecutionContext;
    }

    private List<Cluster> getClusters() {
        return Arrays.asList(getCluster(CLUSTER_ID));
    }

    private Cluster getCluster(long id) {
        Cluster cluster = new Cluster();
        cluster.setId(id);
        return cluster;
    }

    private AbstractMonitor getMonitor() {
        return new AbstractMonitor() {
            @Override
            protected List getMonitored() {
                return Arrays.asList(monitored);
            }

            @Override
            protected void updateLastEvaluated(Monitored monitored) {
            }

            @Override
            public String getIdentifier() {
                return null;
            }

            @Override
            public String getTriggerExpression() {
                return null;
            }

            @Override
            public Class<?> getEvaluatorType(Monitored monitored) {
                return TestExecutor.class;
            }

            @Override
            public EvaluatorContext getContext(Monitored monitored) {
                EvaluatorContext evaluatorContext = mock(EvaluatorContext.class);
                when(evaluatorContext.getItemId()).thenReturn(CLUSTER_ID);
                when(evaluatorContext.getData()).thenReturn(CLUSTER_ID);
                return evaluatorContext;
            }
        };
    }

    private class TestExecutor extends EvaluatorExecutor {

        @Nonnull
        @Override
        public EvaluatorContext getContext() {
            return mock(EvaluatorContext.class);
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void execute() {
        }

        @Override
        public void setContext(EvaluatorContext context) {

        }
    }
}
