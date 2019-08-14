package com.sequenceiq.periscope.modul.rejected;

import static com.sequenceiq.periscope.utils.DelayedAnswer.delayed;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
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
import org.mockito.junit.MockitoJUnitRunner.Silent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AutoscaleStackV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.ClusterManagerVariant;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.model.RejectedThread;
import com.sequenceiq.periscope.modul.rejected.StackCollectorContext.StackCollectorSpringConfig;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.service.StackCollectorService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@RunWith(Silent.class)
@SpringBootTest(classes = StackCollectorSpringConfig.class)
@TestPropertySource(properties = {
        "periscope.monitor.exeutor.registry.timeout=500",
})
public class StackCollectorServiceModulTest extends StackCollectorContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCollectorServiceModulTest.class);

    @Inject
    private StackCollectorService underTest;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    @Inject
    private RejectedThreadService rejectedThreadService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private ClusterService clusterService;

    @Inject
    private HttpNotificationSender httpNotificationSender;

    @Mock
    private CloudbreakInternalCrnClient cloudbreakClient;

    @Mock
    private InternalCrnBuilder internalCrnBuilder;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private CloudbreakServiceCrnEndpoints cloudbreakEndpoint;

    @Mock
    private AutoscaleV4Endpoint autoscaleEndpoint;

    private TestContextManager testContextManager;

    @Before
    public void setUp() throws Exception {
        testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        when(ambariClientProvider.createAmbariClient(any())).thenReturn(ambariClient);
        when(cloudbreakClientConfiguration.cloudbreakInternalCrnClientClient()).thenReturn(cloudbreakClient);
        when(internalCrnBuilder.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(cloudbreakClient.withInternalCrn()).thenReturn(cloudbreakEndpoint);
        when(cloudbreakEndpoint.autoscaleEndpoint()).thenReturn(autoscaleEndpoint);
        when(cloudbreakClient.withInternalCrn().autoscaleEndpoint()).thenReturn(autoscaleEndpoint);

        ReflectionTestUtils.setField(rejectedThreadService, "rejectedThreads", new ConcurrentHashMap<>());
    }

    @Test
    public void testCollectStackDetailsWhenClusterStateRunning() throws IOException, URISyntaxException {
        AutoscaleStackV4Response stack = new AutoscaleStackV4Response();
        stack.setStackId(1L);
        stack.setClusterStatus(Status.AVAILABLE);
        stack.setAmbariServerIp("199.199.199");
        stack.setClusterManagerVariant(ClusterManagerVariant.AMBARI.name());

        Cluster cluster = new Cluster();
        cluster.setState(ClusterState.RUNNING);

        when(autoscaleEndpoint.getAllForAutoscale()).thenReturn(new AutoscaleStackV4Responses(List.of(stack)));
        when(clusterService.findOneByStackId(1L)).thenReturn(cluster);
        when(ambariClient.healthCheck()).thenReturn("RUNNING");

        underTest.collectStackDetails();

        waitForTasksToFinish();

        verify(ambariClient, times(1)).healthCheck();
        verify(httpNotificationSender, times(0)).send(any(Cluster.class), any(History.class));
    }

    @Test
    public void testCollectStackDetailsWhenAmbariNotRunning() throws IOException, URISyntaxException {
        AutoscaleStackV4Response stack = new AutoscaleStackV4Response();
        stack.setStackId(1L);
        stack.setClusterStatus(Status.AVAILABLE);
        stack.setAmbariServerIp("199.199.199");
        stack.setClusterManagerVariant(ClusterManagerVariant.AMBARI.name());

        Cluster cluster = cluster(2L, "someCrn");

        when(autoscaleEndpoint.getAllForAutoscale()).thenReturn(new AutoscaleStackV4Responses(List.of(stack)));
        when(clusterService.findOneByStackId(1L)).thenReturn(cluster);
        when(ambariClient.healthCheck()).thenReturn("NOT_RUNNING");

        underTest.collectStackDetails();

        waitForTasksToFinish();

        verify(ambariClient, times(1)).healthCheck();
    }

    @Test
    public void testCollectStackDetailsWhenRejected() throws IOException, URISyntaxException {
        Cluster cluster = new Cluster();
        cluster.setState(ClusterState.RUNNING);

        when(autoscaleEndpoint.getAllForAutoscale()).thenReturn(
                new AutoscaleStackV4Responses(List.of(autoscaleStackResponse(1L),
                        autoscaleStackResponse(2L),
                        autoscaleStackResponse(3L),
                        autoscaleStackResponse(4L),
                        autoscaleStackResponse(5L))));
        when(clusterService.findOneByStackId(1L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(2L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(3L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(4L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(5L)).thenReturn(cluster);
        when(ambariClient.healthCheck()).thenAnswer(delayed("RUNNING"));

        underTest.collectStackDetails();

        waitForTasksToFinish();

        List<RejectedThread> allRejectedCluster = rejectedThreadService.getAllRejectedCluster();

        Assert.assertFalse(allRejectedCluster.isEmpty());
    }

    @Test
    @Ignore("@Topolyai Gergely should take care of this random failing test.")
    public void testCollectStackDetailsWhenRejectedAndRemoveIt() throws IOException, URISyntaxException {
        Cluster cluster = new Cluster();
        cluster.setState(ClusterState.RUNNING);

        when(autoscaleEndpoint.getAllForAutoscale()).thenReturn(
                new AutoscaleStackV4Responses(List.of(autoscaleStackResponse(1L),
                        autoscaleStackResponse(2L),
                        autoscaleStackResponse(3L),
                        autoscaleStackResponse(4L),
                        autoscaleStackResponse(5L))));
        when(clusterService.findOneByStackId(1L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(2L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(3L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(4L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(5L)).thenReturn(cluster);
        when(ambariClient.healthCheck()).thenAnswer(delayed("RUNNING"));

        underTest.collectStackDetails();

        waitForTasksToFinish();

        List<AutoscaleStackV4Response> stacks = rejectedThreadService.getAllRejectedCluster()
                .stream()
                .map(t -> JsonUtil.readValueOpt(t.getJson(), AutoscaleStackV4Response.class).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        when(autoscaleEndpoint.getAllForAutoscale()).thenReturn(new AutoscaleStackV4Responses(stacks));

        underTest.collectStackDetails();

        waitForTasksToFinish();
        List<RejectedThread> allRejectedCluster = rejectedThreadService.getAllRejectedCluster();

        Assert.assertTrue(allRejectedCluster.isEmpty());
    }

    private Cluster cluster(long id, String stackCrn) {
        Cluster cluster = new Cluster();
        cluster.setId(id);
        cluster.setStackCrn(stackCrn);
        cluster.setId(id);
        cluster.setStackId(id);
        cluster.setClusterManager(new ClusterManager("", "", "", "", ClusterManagerVariant.AMBARI));
        return cluster;
    }

    private AutoscaleStackV4Response autoscaleStackResponse(long stackId) {
        AutoscaleStackV4Response stack = new AutoscaleStackV4Response();
        stack.setStackId(stackId);
        stack.setClusterStatus(Status.AVAILABLE);
        stack.setAmbariServerIp(String.format("199.199.%03d", stackId));
        stack.setClusterManagerVariant(ClusterManagerVariant.AMBARI.name());
        return stack;
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
