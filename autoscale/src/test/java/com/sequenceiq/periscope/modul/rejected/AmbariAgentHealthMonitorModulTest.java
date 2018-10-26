package com.sequenceiq.periscope.modul.rejected;

import static com.sequenceiq.periscope.utils.DelayedAnswer.delayed;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner.Silent;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.autoscale.AutoscaleEndpoint;
import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.RejectedThread;
import com.sequenceiq.periscope.modul.rejected.RejectedThreadContext.SpringConfig;
import com.sequenceiq.periscope.monitor.AmbariAgentHealthMonitor;
import com.sequenceiq.periscope.monitor.MonitorContext;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@RunWith(Silent.class)
@SpringBootTest(classes = SpringConfig.class)
public class AmbariAgentHealthMonitorModulTest extends RejectedThreadContext {

    @Inject
    private AmbariAgentHealthMonitor underTest;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    @Inject
    private ThreadPoolExecutor executorService;

    @Inject
    private RejectedThreadService rejectedThreadService;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    @Mock
    private CloudbreakClient cloudbreakClient;

    private TestContextManager testContextManager;

    @Before
    public void setUp() throws Exception {
        testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        JobDataMap map = new JobDataMap(Collections.singletonMap(MonitorContext.APPLICATION_CONTEXT.name(), applicationContext));
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobDataMap()).thenReturn(map);

        when(ambariClientProvider.createAmbariClient(any())).thenReturn(ambariClient);
        when(cloudbreakClientConfiguration.cloudbreakClient()).thenReturn(cloudbreakClient);

        ReflectionTestUtils.setField(rejectedThreadService, "rejectedThreads", new ConcurrentHashMap<>());
    }

    @Test
    public void testWhenHeartBeatCritical() {
        AutoscaleEndpoint autoscaleEndpoint = mock(AutoscaleEndpoint.class);
        Cluster cluster = new Cluster();
        long clusterId = 1L;
        long stackId = 0L;
        cluster.setId(clusterId);
        cluster.setStackId(stackId);

        when(jobDetail.getKey()).thenReturn(JobKey.jobKey("test-heart-beat-critical"));
        when(clusterService.findById(clusterId)).thenReturn(cluster);
        when(cloudbreakClient.autoscaleEndpoint()).thenReturn(autoscaleEndpoint);

        Map<String, Object> map = new HashMap<>();
        map.put("state", "CRITICAL");
        map.put("host_name", "hostname-recovery");
        when(ambariClient.getAlert("ambari_server_agent_heartbeat")).thenReturn(Collections.singletonList(map));

        List<Cluster> clusters = Collections.singletonList(cluster);
        when(clusterService.findAllByStateAndNode(ClusterState.RUNNING, null)).thenReturn(clusters);
        underTest.execute(context);

        waitForTasksToFinish();

        verify(autoscaleEndpoint, times(1)).failureReport(eq(stackId), any(FailureReport.class));
    }

    @Test
    public void testWhenThreadPoolRejected() {
        Cluster cluster1 = cluster(1L);
        Cluster cluster2 = cluster(2L);
        Cluster cluster3 = cluster(3L);
        Cluster cluster4 = cluster(4L);
        Cluster cluster5 = cluster(5L);

        when(jobDetail.getKey()).thenReturn(JobKey.jobKey("test-rejected-testWhenThreadPoolRejected"));
        when(clusterService.findById(1L)).thenReturn(cluster1);
        when(clusterService.findById(2L)).thenReturn(cluster2);
        when(clusterService.findById(3L)).thenReturn(cluster3);
        when(clusterService.findById(4L)).thenReturn(cluster4);
        when(clusterService.findById(5L)).thenReturn(cluster5);

        when(ambariClient.getAlert(anyString())).thenAnswer(delayed(Collections.emptyList()));

        List<Cluster> clusters = Arrays.asList(cluster1, cluster2, cluster3, cluster4, cluster5);
        when(clusterService.findAllByStateAndNode(ClusterState.RUNNING, null)).thenReturn(clusters);

        underTest.execute(context);

        waitForTasksToFinish();

        List<RejectedThread> allRejectedCluster = rejectedThreadService.getAllRejectedCluster();

        Assert.assertFalse(allRejectedCluster.isEmpty());

    }

    @Test
    public void testWhenThreadPoolRejectedAndSubmitAgain() {
        long stackId = 1L;
        Cluster cluster = cluster(stackId);

        when(jobDetail.getKey()).thenReturn(JobKey.jobKey("test-rejected-testWhenThreadPoolRejectedAndSubmitAgain"));
        when(clusterService.findById(stackId)).thenReturn(cluster);

        when(ambariClient.getAlert("ambari_server_agent_heartbeat")).thenAnswer(delayed(Collections.emptyList()));

        List<Cluster> clusters = Arrays.asList(cluster, cluster(2L), cluster(3L), cluster(4L), cluster(5L));
        when(clusterService.findAllByStateAndNode(ClusterState.RUNNING, null)).thenReturn(clusters);
        underTest.execute(context);
        waitForTasksToFinish();

        List<Cluster> clusters1 = rejectedThreadService.getAllRejectedCluster()
                .stream()
                .map(t -> JsonUtil.readValueOpt(t.getJson(), Cluster.class).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        when(clusterService.findAllByStateAndNode(ClusterState.RUNNING, null)).thenReturn(clusters1);
        when(ambariClient.getAlert("ambari_server_agent_heartbeat")).thenReturn(Collections.emptyList());
        underTest.execute(context);

        waitForTasksToFinish();

        List<RejectedThread> allRejectedCluster = rejectedThreadService.getAllRejectedCluster();

        Assert.assertTrue(allRejectedCluster.isEmpty());

    }

    @Test
    public void testWhenThreadPoolRejectedAndCountMoreThanOne() {
        Cluster cluster1 = cluster(1L);
        Cluster cluster2 = cluster(2L);
        Cluster cluster3 = cluster(3L);
        Cluster cluster4 = cluster(4L);
        Cluster cluster5 = cluster(5L);

        when(jobDetail.getKey()).thenReturn(JobKey.jobKey("test-rejected-testWhenThreadPoolRejectedAndCountMoreThanOne"));
        when(clusterService.findById(1L)).thenReturn(cluster1);
        when(clusterService.findById(2L)).thenReturn(cluster2);
        when(clusterService.findById(3L)).thenReturn(cluster3);
        when(clusterService.findById(4L)).thenReturn(cluster4);
        when(clusterService.findById(5L)).thenReturn(cluster5);

        when(ambariClient.getAlert("ambari_server_agent_heartbeat")).thenAnswer(delayed(Collections.emptyList()));

        List<Cluster> clusters = Arrays.asList(cluster1, cluster2, cluster3, cluster4, cluster5);
        when(clusterService.findAllByStateAndNode(ClusterState.RUNNING, null)).thenReturn(clusters);
        underTest.execute(context);
        waitForTasksToFinish();

        when(clusterService.findAllByStateAndNode(ClusterState.RUNNING, null)).thenReturn(clusters);
        when(ambariClient.getAlert("ambari_server_agent_heartbeat")).thenAnswer(delayed(Collections.emptyList()));
        underTest.execute(context);

        waitForTasksToFinish();

        List<RejectedThread> allRejectedCluster = rejectedThreadService.getAllRejectedCluster();

        Assert.assertThat(allRejectedCluster, not(empty()));
        RejectedThread rejectedThread = allRejectedCluster.get(0);
        Assert.assertEquals(5L, rejectedThread.getId());
        Assert.assertEquals(2L, rejectedThread.getRejectedCount());

    }

    private Cluster cluster(long stackId) {
        Cluster cluster = new Cluster();
        cluster.setId(stackId);
        cluster.setStackId(stackId);
        return cluster;
    }

    private void waitForTasksToFinish() {
        ExecutorServiceWithRegistry executorServiceWithRegistry = applicationContext.getBean(ExecutorServiceWithRegistry.class);
        while (executorServiceWithRegistry.activeCount() > 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignore) {
            }
        }
    }
}
