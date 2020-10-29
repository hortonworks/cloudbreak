package com.sequenceiq.periscope.monitor.handler;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.monitor.ScalingHandlerUtil;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.service.ClusterService;

@RunWith(MockitoJUnitRunner.class)
public class ScalingHandlerTest {

    @InjectMocks
    private ScalingHandler underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ScalingHandlerUtil scalingHandlerUtil;

    @Test
    public void testOnApplicationEventWhenHasTwoAlerts() {
        Cluster cluster = new Cluster();
        cluster.setUser(new PeriscopeUser());
        cluster.setId(2L);
        MetricAlert alert1 = new MetricAlert();
        alert1.setCluster(cluster);
        MetricAlert alert2 = new MetricAlert();
        alert2.setCluster(cluster);
        ScalingEvent event = new ScalingEvent(List.of(alert1, alert2));

        when(scalingHandlerUtil.isCooldownElapsed(cluster)).thenReturn(true).thenReturn(false);
        when(clusterService.findById(2L)).thenReturn(cluster);
        underTest.onApplicationEvent(event);

        verify(scalingHandlerUtil, times(1)).scaleIfNeed(cluster, alert1);
        verify(scalingHandlerUtil, times(0)).scaleIfNeed(cluster, alert2);
    }
}
