package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.api.model.AdjustmentType.LOAD_BASED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.RejectedThreadService;

@ExtendWith(MockitoExtension.class)
public class ScalingHandlerTest {

    private static final long AUTOSCALE_CLUSTER_ID = 101L;

    @InjectMocks
    private ScalingHandler underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private RejectedThreadService rejectedThreadService;

    @Mock
    private ExecutorService executorService;

    @Mock
    private HistoryService historyService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private ApplicationContext applicationContext;

    private ScalingPolicy scalingPolicyMock = mock(ScalingPolicy.class);

    private ScalingEvent scalingEventMock = mock(ScalingEvent.class);

    private Runnable runnableMock = mock(Runnable.class);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnApplicationEventWhenLoadAlertDecommissionNodes() {
        LoadAlert loadAlertMock = mock(LoadAlert.class);
        Cluster cluster = getARunningCluster();

        int hostGroupNodeCount = 10;
        List nodeIds = List.of("nodeId1", "nodeId2", "nodeId3");
        int expectedNodeCount = hostGroupNodeCount - nodeIds.size();

        when(scalingEventMock.getAlert()).thenReturn(loadAlertMock);
        when(loadAlertMock.getCluster()).thenReturn(cluster);
        when(loadAlertMock.getScalingPolicy()).thenReturn(scalingPolicyMock);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(scalingPolicyMock.getAdjustmentType()).thenReturn(LOAD_BASED);
        when(scalingEventMock.getHostGroupNodeCount()).thenReturn(hostGroupNodeCount);
        when(scalingEventMock.getDesiredAbsoluteHostGroupNodeCount()).thenReturn(expectedNodeCount);
        when(scalingEventMock.getDecommissionNodeIds()).thenReturn(nodeIds);
        when(applicationContext.getBean("ScalingRequest", cluster, scalingPolicyMock,
                hostGroupNodeCount, expectedNodeCount, nodeIds)).thenReturn(runnableMock);

        underTest.onApplicationEvent(scalingEventMock);

        verify(rejectedThreadService).remove(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).save(cluster);
        verify(executorService).submit(runnableMock);
    }

    @Test
    public void testOnApplicationEventWhenNoScaling() {
        Cluster cluster = getARunningCluster();
        TimeAlert timeAlertMock = mock(TimeAlert.class);

        when(scalingEventMock.getAlert()).thenReturn(timeAlertMock);
        when(scalingEventMock.getHostGroupNodeCount()).thenReturn(2);
        when(scalingEventMock.getDesiredAbsoluteHostGroupNodeCount()).thenReturn(2);
        when(scalingPolicyMock.getHostGroup()).thenReturn("compute");
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(messagesService.getMessage(anyString(), any(List.class))).thenReturn("");

        when(timeAlertMock.getCluster()).thenReturn(cluster);
        when(timeAlertMock.getScalingPolicy()).thenReturn(scalingPolicyMock);
        when(timeAlertMock.getName()).thenReturn("testalert");
        when(timeAlertMock.getAlertType()).thenCallRealMethod();

        underTest.onApplicationEvent(scalingEventMock);

        verify(historyService, times(1)).createEntry(
                eq(ScalingStatus.SUCCESS), anyString(), eq(2), eq(0), eq(scalingPolicyMock));
        verify(clusterService, never()).save(cluster);
        verify(applicationContext, never()).getBean("ScalingRequest");
    }

    public static Stream<Arguments> dataClusterScaling() {
        return Stream.of(
                //TestCase, CurrentHostGroupCount,ScalingAdjustment,ExpectedScalingCount
                Arguments.of("SCALE_UP",  2,  5, 7),
                Arguments.of("SCALE_UP", 10, 40, 50),
                Arguments.of("SCALE_UP", 10, 15, 25),
                Arguments.of("SCALE_UP", 10, 40, 50),
                Arguments.of("SCALE_DOWN", 10, -5, 5),
                Arguments.of("SCALE_DOWN", 15, -14, 1),
                Arguments.of("SCALE_DOWN", 10, -7, 3)
        );
    }

    @ParameterizedTest(name = "{0}: With currentHostGroupCount={1}, scalingAdjustment ={2}, expectedScalingCount={3} ")
    @MethodSource("dataClusterScaling")
    public void testClusterScaling(String testType, int currentHostGroupCount,
            int scalingAdjustment, int expectedScalingCount) {
        LoadAlert loadAlertMock = mock(LoadAlert.class);
        validateClusterScaling(loadAlertMock, currentHostGroupCount, scalingAdjustment, expectedScalingCount);
    }

    private void validateClusterScaling(BaseAlert baseAlertMock,
            int currentHostGroupCount, int scalingAdjument, int expectedScaleUpCount) {
        MockitoAnnotations.initMocks(this);
        Cluster cluster = getARunningCluster();

        when(scalingEventMock.getAlert()).thenReturn(baseAlertMock);
        when(scalingEventMock.getHostGroupNodeCount()).thenReturn(currentHostGroupCount);
        when(scalingEventMock.getDesiredAbsoluteHostGroupNodeCount()).thenReturn(currentHostGroupCount + scalingAdjument);

        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(baseAlertMock.getCluster()).thenReturn(cluster);
        when(baseAlertMock.getScalingPolicy()).thenReturn(scalingPolicyMock);

        when(scalingEventMock.getDecommissionNodeIds()).thenCallRealMethod();
        when(applicationContext.getBean("ScalingRequest", cluster, scalingPolicyMock,
                currentHostGroupCount, expectedScaleUpCount, List.of())).thenReturn(runnableMock);

        underTest.onApplicationEvent(scalingEventMock);

        verify(rejectedThreadService).remove(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).save(cluster);
        verify(executorService).submit(runnableMock);
    }

    private Cluster getARunningCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setState(ClusterState.RUNNING);
        cluster.setLastScalingActivity(Instant.now()
                .minus(45, ChronoUnit.MINUTES).toEpochMilli());
        return cluster;
    }
}
