package com.sequenceiq.periscope.modul.rejected;

import static com.sequenceiq.periscope.utils.DelayedAnswer.delayed;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.model.RejectedThread;
import com.sequenceiq.periscope.modul.rejected.RejectedThreadContext.SpringConfig;
import com.sequenceiq.periscope.monitor.ClusterManagerHostHealthMonitor;
import com.sequenceiq.periscope.monitor.MonitorContext;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.repository.FailedNodeRepository;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;

@RunWith(MockitoJUnitRunner.Silent.class)
@SpringBootTest(classes = SpringConfig.class)
@Ignore("Ignored since ClusterManagerHostHealthMonitor is disabled by default.")
public class ClusterManagerHostHealthMonitorModuleTest extends RejectedThreadContext {

    @Inject
    private ClusterManagerHostHealthMonitor underTest;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private RejectedThreadService rejectedThreadService;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    @Inject
    private FailedNodeRepository failedNodeRepository;

    @Before
    public void setUp() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        JobDataMap map = new JobDataMap(Collections.singletonMap(MonitorContext.APPLICATION_CONTEXT.name(), applicationContext));
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobDataMap()).thenReturn(map);

        when(ambariClientProvider.createAmbariClient(any())).thenReturn(ambariClient);

        ReflectionTestUtils.setField(rejectedThreadService, "rejectedThreads", new ConcurrentHashMap<>());
    }

    @Test
    public void testWhenHeartBeatCritical() {
        Cluster cluster = new Cluster();
        long clusterId = 1L;
        String stackCrn = "someCrn";
        cluster.setId(clusterId);
        cluster.setStackCrn(stackCrn);
        cluster.setClusterManager(new ClusterManager("", "", "", "", ClusterManagerVariant.AMBARI));

        when(jobDetail.getKey()).thenReturn(JobKey.jobKey("test-heart-beat-critical"));
        when(clusterService.findById(clusterId)).thenReturn(cluster);

        Map<String, Object> map = new HashMap<>();
        map.put("state", "CRITICAL");
        map.put("host_name", "hostname-recovery");
        when(ambariClient.getAlert("ambari_server_agent_heartbeat")).thenReturn(Collections.singletonList(map));

        List<Cluster> clusters = Collections.singletonList(cluster);
        when(clusterService.findAllByStateAndNode(ClusterState.RUNNING, null)).thenReturn(clusters);
        underTest.execute(context);

        waitForTasksToFinish();

        verify(failedNodeRepository, times(1)).saveAll(any());
    }

    @Test
    public void testWhenThreadPoolRejected() {
        Cluster cluster1 = cluster(1L, "someCrn1");
        Cluster cluster2 = cluster(2L, "someCrn2");
        Cluster cluster3 = cluster(3L, "someCrn3");
        Cluster cluster4 = cluster(4L, "someCrn4");
        Cluster cluster5 = cluster(5L, "someCrn5");

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

    @Ignore
    @Test
    public void testWhenThreadPoolRejectedAndSubmitAgain() {
        long stackId = 1L;
        Cluster cluster = cluster(stackId, "somecrn");

        when(jobDetail.getKey()).thenReturn(JobKey.jobKey("test-rejected-testWhenThreadPoolRejectedAndSubmitAgain"));
        when(clusterService.findById(stackId)).thenReturn(cluster);

        when(ambariClient.getAlert("ambari_server_agent_heartbeat")).thenAnswer(delayed(Collections.emptyList()));

        List<Cluster> clusters = Arrays.asList(cluster, cluster(2L, "someCrn2"), cluster(3L, "someCrn3"), cluster(4L, "someCrn4"), cluster(5L, "someCrn5"));
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

    @Ignore
    @Test
    public void testWhenThreadPoolRejectedAndCountMoreThanOne() {
        Cluster cluster1 = cluster(1L, "someCrn1");
        Cluster cluster2 = cluster(2L, "someCrn2");
        Cluster cluster3 = cluster(3L, "someCrn3");
        Cluster cluster4 = cluster(4L, "someCrn4");
        Cluster cluster5 = cluster(5L, "someCrn5");

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

    private Cluster cluster(long stackId, String stackCrn) {
        Cluster cluster = new Cluster();
        cluster.setId(stackId);
        cluster.setStackCrn(stackCrn);
        cluster.setStackId(stackId);
        cluster.setClusterManager(new ClusterManager("", "", "", "", ClusterManagerVariant.AMBARI));
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
