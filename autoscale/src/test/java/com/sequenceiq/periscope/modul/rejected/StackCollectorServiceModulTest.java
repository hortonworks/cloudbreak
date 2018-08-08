package com.sequenceiq.periscope.modul.rejected;

import static com.sequenceiq.periscope.utils.DelayedAnswer.delayed;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner.Silent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;

import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.model.RejectedThread;
import com.sequenceiq.periscope.modul.rejected.StackCollectorContext.StackCollectorSpringConfig;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.service.StackCollectorService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@RunWith(Silent.class)
@SpringBootTest(classes = StackCollectorSpringConfig.class)
public class StackCollectorServiceModulTest extends StackCollectorContext {

    @Inject
    private StackCollectorService underTest;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ThreadPoolExecutor executorService;

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
    private CloudbreakClient cloudbreakClient;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private StackV1Endpoint stackV1Endpoint;

    private TestContextManager testContextManager;

    @Before
    public void setUp() throws Exception {
        testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        when(ambariClientProvider.createAmbariClient(any())).thenReturn(ambariClient);
        when(cloudbreakClientConfiguration.cloudbreakClient()).thenReturn(cloudbreakClient);
        when(cloudbreakClient.stackV1Endpoint()).thenReturn(stackV1Endpoint);
    }

    @Test
    public void testCollectStackDetailsWhenClusterStateRunning() {
        AutoscaleStackResponse stack = new AutoscaleStackResponse();
        stack.setStackId(1L);
        stack.setClusterStatus(Status.AVAILABLE);
        stack.setAmbariServerIp("199.199.199");

        Cluster cluster = new Cluster();
        cluster.setState(ClusterState.RUNNING);

        when(stackV1Endpoint.getAllForAutoscale()).thenReturn(Collections.singleton(stack));
        when(clusterService.findOneByStackId(1L)).thenReturn(cluster);
        when(ambariClient.healthCheck()).thenReturn("RUNNING");

        underTest.collectStackDetails();

        waitForThreadPool();

        verify(ambariClient, times(1)).healthCheck();
        verify(httpNotificationSender, times(0)).send(any(History.class));
    }

    @Test
    public void testCollectStackDetailsWhenAmbariNotRunning() {
        AutoscaleStackResponse stack = new AutoscaleStackResponse();
        stack.setStackId(1L);
        stack.setClusterStatus(Status.AVAILABLE);
        stack.setAmbariServerIp("199.199.199");

        Cluster cluster = cluster(2L);

        when(stackV1Endpoint.getAllForAutoscale()).thenReturn(Collections.singleton(stack));
        when(clusterService.findOneByStackId(1L)).thenReturn(cluster);
        when(ambariClient.healthCheck()).thenReturn("NOT_RUNNING");

        underTest.collectStackDetails();

        waitForThreadPool();

        verify(ambariClient, times(1)).healthCheck();
    }

    @Test
    public void testCollectStackDetailsWhenRejected() {
        AutoscaleStackResponse stack = autoscaleStackResponse(1L);

        Cluster cluster = new Cluster();
        cluster.setState(ClusterState.RUNNING);

        when(stackV1Endpoint.getAllForAutoscale()).thenReturn(Sets.newHashSet(stack, autoscaleStackResponse(2L), autoscaleStackResponse(3L),
                autoscaleStackResponse(4L), autoscaleStackResponse(5L)));
        when(clusterService.findOneByStackId(1L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(2L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(3L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(4L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(5L)).thenReturn(cluster);
        when(ambariClient.healthCheck()).thenAnswer(delayed("RUNNING"));

        underTest.collectStackDetails();

        waitForThreadPool();

        List<RejectedThread> allRejectedCluster = rejectedThreadService.getAllRejectedCluster();

        Assert.assertFalse(allRejectedCluster.isEmpty());
    }

    @Test
    public void testCollectStackDetailsWhenRejectedAndRemoveIt() {
        AutoscaleStackResponse stack = autoscaleStackResponse(1L);

        Cluster cluster = new Cluster();
        cluster.setState(ClusterState.RUNNING);

        when(stackV1Endpoint.getAllForAutoscale()).thenReturn(Sets.newHashSet(stack, autoscaleStackResponse(2L), autoscaleStackResponse(3L),
                autoscaleStackResponse(4L), autoscaleStackResponse(5L)));
        when(clusterService.findOneByStackId(1L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(2L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(3L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(4L)).thenReturn(cluster);
        when(clusterService.findOneByStackId(5L)).thenReturn(cluster);
        when(ambariClient.healthCheck()).thenAnswer(delayed("RUNNING"));

        underTest.collectStackDetails();

        waitForThreadPool();

        Set<AutoscaleStackResponse> stacks = rejectedThreadService.getAllRejectedCluster()
                .stream()
                .map(t -> JsonUtil.readValueOpt(t.getJson(), AutoscaleStackResponse.class).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        when(stackV1Endpoint.getAllForAutoscale()).thenReturn(stacks);

        underTest.collectStackDetails();

        List<RejectedThread> allRejectedCluster = rejectedThreadService.getAllRejectedCluster();

        Assert.assertTrue(allRejectedCluster.isEmpty());
    }

    private Cluster cluster(long stackId) {
        Cluster cluster = new Cluster();
        cluster.setId(stackId);
        cluster.setStackId(stackId);
        cluster.setUser(new PeriscopeUser());
        return cluster;
    }

    private AutoscaleStackResponse autoscaleStackResponse(long stackId) {
        AutoscaleStackResponse stack = new AutoscaleStackResponse();
        stack.setStackId(stackId);
        stack.setClusterStatus(Status.AVAILABLE);
        stack.setAmbariServerIp("199.199.199");
        return stack;
    }

    private void waitForThreadPool() {
        while (executorService.getActiveCount() != 0) ;
    }
}
