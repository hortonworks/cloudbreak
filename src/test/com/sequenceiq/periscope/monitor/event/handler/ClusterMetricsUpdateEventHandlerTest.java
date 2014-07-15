package com.sequenceiq.periscope.monitor.event.handler;

import static org.mockito.Mockito.when;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.periscope.monitor.event.ClusterMetricsUpdateEvent;

@RunWith(MockitoJUnitRunner.class)
public class ClusterMetricsUpdateEventHandlerTest {

    @InjectMocks
    private ClusterMetricsUpdateEventHandler handler;

    @Mock
    private ClusterMetricsUpdateEvent event;
    @Mock
    private ClusterMetricsInfo metrics;

    @Before
    public void init() {
        ReflectionTestUtils.setField(handler, "minNodeCount", 3);
    }

    @Test
    public void testShouldAddNewNodes() {
        when(event.getClusterMetricsInfo()).thenReturn(metrics);
        when(metrics.getTotalMB()).thenReturn(Long.valueOf(10_000));
        when(metrics.getAvailableMB()).thenReturn(Long.valueOf(1_000));

        handler.onApplicationEvent(event);
    }
}
