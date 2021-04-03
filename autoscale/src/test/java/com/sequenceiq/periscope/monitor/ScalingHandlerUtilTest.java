package com.sequenceiq.periscope.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.api.endpoint.autoscale.AutoscaleEndpoint;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.monitor.executor.LoggedExecutorService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;

@RunWith(MockitoJUnitRunner.class)
public class ScalingHandlerUtilTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private RejectedThreadService rejectedThreadService;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private AutoscaleEndpoint autoscaleEndpoint;

    @Mock
    private ClusterService clusterService;

    @Mock
    private LoggedExecutorService loggedExecutorService;

    @Mock
    private Clock clock;

    @InjectMocks
    private ScalingHandlerUtil underTest;

    @Test
    public void testIsCooldownElapsedWhenLastActivityIsZero() {
        Cluster cluster = new Cluster();
        cluster.setLastScalingActivity(0L);
        boolean actual = underTest.isCooldownElapsed(cluster);
        assertTrue(actual);
    }

    @Test
    public void testIsCooldownElapsedWhenCoolDownLessThenZero() {
        Cluster cluster = new Cluster();
        cluster.setCoolDown(1);
        cluster.setLastScalingActivity(1000L);

        when(clock.getCurrentTime()).thenReturn(70000L);

        boolean actual = underTest.isCooldownElapsed(cluster);
        assertTrue(actual);
    }

    @Test
    public void testIsCooldownElapsedWhenCoolDownMoreThenZero() {
        Cluster cluster = new Cluster();
        cluster.setCoolDown(1);
        cluster.setLastScalingActivity(7000L);

        when(clock.getCurrentTime()).thenReturn(1000L);

        boolean actual = underTest.isCooldownElapsed(cluster);
        assertFalse(actual);
    }

    @Test
    public void testIsCooldownElapsedWhenCoolDownIsZero() {
        Cluster cluster = new Cluster();
        cluster.setCoolDown(1);
        cluster.setLastScalingActivity(1000L);

        when(clock.getCurrentTime()).thenReturn(61000L);

        boolean actual = underTest.isCooldownElapsed(cluster);
        assertTrue(actual);
    }

    @Test
    public void testScaleIfNeedWhenTotalNodesEqualsToDesiredNodeCount() {
        Cluster cluster = new Cluster();
        cluster.setStackId(10L);
        cluster.setMinSize(1);
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(AdjustmentType.EXACT);
        policy.setScalingAdjustment(1);
        policy.setHostGroup("hg");
        MetricAlert alert = new MetricAlert();
        alert.setScalingPolicy(policy);
        when(cloudbreakClient.autoscaleEndpoint()).thenReturn(autoscaleEndpoint);
        when(autoscaleEndpoint.getHostMetadataCountForAutoscale(10L, "hg")).thenReturn(1L);
        underTest.scaleIfNeed(cluster, alert);
        verify(clusterService, times(0)).save(cluster);
    }

    @Test
    public void testScaleIfNeedWhenTotalNodesNotEqualsToDesiredNodeCount() {
        Runnable runnable = mock(Runnable.class);

        Cluster cluster = new Cluster();
        cluster.setStackId(10L);
        cluster.setMinSize(1);
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(AdjustmentType.EXACT);
        policy.setScalingAdjustment(2);
        policy.setHostGroup("hg");
        MetricAlert alert = new MetricAlert();
        alert.setScalingPolicy(policy);
        when(cloudbreakClient.autoscaleEndpoint()).thenReturn(autoscaleEndpoint);
        when(autoscaleEndpoint.getHostMetadataCountForAutoscale(10L, "hg")).thenReturn(1L);
        when(applicationContext.getBean("ScalingRequest", cluster, policy, 1, 2)).thenReturn(runnable);
        underTest.scaleIfNeed(cluster, alert);
        verify(clusterService, times(1)).updateLastScalingActivity(cluster);
        verify(loggedExecutorService, times(1)).submit("ScalingHandler", runnable);
    }

    @Test
    public void testGetDesiredCountWhenExact() {
        Cluster cluster = new Cluster();
        cluster.setStackId(10L);
        cluster.setMinSize(1);
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(AdjustmentType.EXACT);
        policy.setScalingAdjustment(1);

        int actual = underTest.getDesiredNodeCount(cluster, policy, 1);

        assertEquals(1, actual);
    }

    @Test
    public void testGetDesiredCountWhenNodeCount() {
        Cluster cluster = new Cluster();
        cluster.setStackId(10L);
        cluster.setMinSize(1);
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(AdjustmentType.NODE_COUNT);
        policy.setScalingAdjustment(1);

        int actual = underTest.getDesiredNodeCount(cluster, policy, 1);

        assertEquals(2, actual);
    }

    @Test
    public void testGetDesiredCountWhenPercentage() {
        Cluster cluster = new Cluster();
        cluster.setStackId(10L);
        cluster.setMinSize(1);
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(AdjustmentType.NODE_COUNT);
        policy.setScalingAdjustment(20);

        int actual = underTest.getDesiredNodeCount(cluster, policy, 5);

        assertEquals(25, actual);
    }

    @Test
    public void testGetDesiredCountWhenMinSizeMoreThanDesiredCount() {
        Cluster cluster = new Cluster();
        cluster.setStackId(10L);
        cluster.setMinSize(5);
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(AdjustmentType.EXACT);
        policy.setScalingAdjustment(1);

        int actual = underTest.getDesiredNodeCount(cluster, policy, 1);

        assertEquals(5, actual);
    }

    @Test
    public void testGetDesiredCountWhenMaxSizeLessThanDesiredCount() {
        Cluster cluster = new Cluster();
        cluster.setStackId(10L);
        cluster.setMinSize(5);
        cluster.setMaxSize(10);
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(AdjustmentType.EXACT);
        policy.setScalingAdjustment(30);

        int actual = underTest.getDesiredNodeCount(cluster, policy, 1);

        assertEquals(10, actual);
    }
}
