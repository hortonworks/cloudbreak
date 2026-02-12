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

import org.junit.jupiter.api.BeforeEach;
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
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.model.ScalingAdjustmentType;
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
    private ExecutorService executorTimeMonitorService;

    @Mock
    private HistoryService historyService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private ApplicationContext applicationContext;

    private ScalingPolicy scalingPolicyMock = mock(ScalingPolicy.class);

    private ScalingEvent scalingEventMock = mock(ScalingEvent.class);

    private Runnable runnableMock = mock(Runnable.class);

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnApplicationEventWhenLoadAlertDecommissionNodes() {
        LoadAlert loadAlertMock = mock(LoadAlert.class);
        Cluster cluster = getARunningCluster();

        int clusterNodeSize = 100;
        int hostGroupNodeCount = 10;
        List nodeIds = List.of("nodeId1", "nodeId2", "nodeId3");
        int expectedNodeCount = hostGroupNodeCount - nodeIds.size();
        ScalingAdjustmentType scalingType = ScalingAdjustmentType.REGULAR;
        Long activityId = 1234L;

        when(scalingEventMock.getAlert()).thenReturn(loadAlertMock);
        when(scalingEventMock.getAlert().getAlertType()).thenReturn(AlertType.LOAD);
        when(loadAlertMock.getCluster()).thenReturn(cluster);
        when(loadAlertMock.getScalingPolicy()).thenReturn(scalingPolicyMock);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(scalingPolicyMock.getAdjustmentType()).thenReturn(LOAD_BASED);
        when(scalingEventMock.getExistingClusterNodeCount()).thenReturn(clusterNodeSize);
        when(scalingEventMock.getExistingHostGroupNodeCount()).thenReturn(hostGroupNodeCount);
        when(scalingEventMock.getDesiredAbsoluteHostGroupNodeCount()).thenReturn(expectedNodeCount);
        when(scalingEventMock.getDecommissionNodeIds()).thenReturn(nodeIds);
        when(scalingEventMock.getExistingServiceHealthyHostGroupNodeCount()).thenReturn(hostGroupNodeCount);
        when(scalingEventMock.getScalingAdjustmentType()).thenReturn(scalingType);
        when(scalingEventMock.getActivityId()).thenReturn(activityId);
        when(applicationContext.getBean("ScalingRequest", cluster, scalingPolicyMock,
                clusterNodeSize, hostGroupNodeCount, expectedNodeCount, nodeIds, hostGroupNodeCount, scalingType, activityId)).thenReturn(runnableMock);

        underTest.onApplicationEvent(scalingEventMock);

        verify(rejectedThreadService).remove(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setLastScalingActivity(eq(AUTOSCALE_CLUSTER_ID), anyLong());
        verify(executorService).submit(runnableMock);
    }

    @Test
    public void testOnApplicationEventWhenTimeAlertDecommissionNodes() {
        TimeAlert timeAlertMock = mock(TimeAlert.class);
        Cluster cluster = getARunningCluster();

        int clusterNodeSize = 100;
        int hostGroupNodeCount = 10;
        List nodeIds = List.of("nodeId1", "nodeId2", "nodeId3");
        int expectedNodeCount = hostGroupNodeCount - nodeIds.size();
        ScalingAdjustmentType scalingType = ScalingAdjustmentType.REGULAR;
        Long activityId = 1234L;

        when(scalingEventMock.getAlert()).thenReturn(timeAlertMock);
        when(scalingEventMock.getAlert().getAlertType()).thenReturn(AlertType.TIME);
        when(timeAlertMock.getCluster()).thenReturn(cluster);
        when(timeAlertMock.getScalingPolicy()).thenReturn(scalingPolicyMock);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(scalingPolicyMock.getAdjustmentType()).thenReturn(LOAD_BASED);
        when(scalingEventMock.getExistingClusterNodeCount()).thenReturn(clusterNodeSize);
        when(scalingEventMock.getExistingHostGroupNodeCount()).thenReturn(hostGroupNodeCount);
        when(scalingEventMock.getDesiredAbsoluteHostGroupNodeCount()).thenReturn(expectedNodeCount);
        when(scalingEventMock.getDecommissionNodeIds()).thenReturn(nodeIds);
        when(scalingEventMock.getExistingServiceHealthyHostGroupNodeCount()).thenReturn(hostGroupNodeCount);
        when(scalingEventMock.getScalingAdjustmentType()).thenReturn(scalingType);
        when(scalingEventMock.getActivityId()).thenReturn(activityId);
        when(applicationContext.getBean("ScalingRequest", cluster, scalingPolicyMock,
                clusterNodeSize, hostGroupNodeCount, expectedNodeCount, nodeIds, hostGroupNodeCount, scalingType, activityId)).thenReturn(runnableMock);

        underTest.onApplicationEvent(scalingEventMock);

        verify(rejectedThreadService).remove(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setLastScalingActivity(eq(AUTOSCALE_CLUSTER_ID), anyLong());
        verify(executorTimeMonitorService).submit(runnableMock);
    }

    @Test
    public void testOnApplicationEventWhenNoScaling() {
        Cluster cluster = getARunningCluster();
        TimeAlert timeAlertMock = mock(TimeAlert.class);

        when(scalingEventMock.getAlert()).thenReturn(timeAlertMock);
        when(scalingEventMock.getExistingHostGroupNodeCount()).thenReturn(2);
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
        verify(clusterService, never()).setLastScalingActivity(eq(AUTOSCALE_CLUSTER_ID), anyLong());
        verify(applicationContext, never()).getBean("ScalingRequest");
    }

    public static Stream<Arguments> dataClusterScaling() {
        return Stream.of(
                //TestCase, CurrentClusterNodeCount, CurrentHostGroupCount,ScalingAdjustment,ExpectedScalingCount
                Arguments.of("SCALE_UP", 10, 2,  5, 7),
                Arguments.of("SCALE_UP", 10, 10, 40, 50),
                Arguments.of("SCALE_UP", 10, 10, 15, 25),
                Arguments.of("SCALE_UP", 10, 10, 40, 50),
                Arguments.of("SCALE_DOWN", 10, 10, -5, 5),
                Arguments.of("SCALE_DOWN", 10, 15, -14, 1),
                Arguments.of("SCALE_DOWN", 10, 10, -7, 3)
        );
    }

    @ParameterizedTest(name = "{0}: With currentClusterNodeCount={1}, currentHostGroupCount={2}, scalingAdjustment ={3}, expectedScalingCount={4} ")
    @MethodSource("dataClusterScaling")
    public void testClusterScaling(String testType, int currentClusterNodeCount, int currentHostGroupCount,
            int scalingAdjustment, int expectedScalingCount) {
        LoadAlert loadAlertMock = mock(LoadAlert.class);
        validateClusterScaling(loadAlertMock, currentClusterNodeCount, currentHostGroupCount, scalingAdjustment, expectedScalingCount);
    }

    private void validateClusterScaling(BaseAlert baseAlertMock,
            int currentClusterNodeCount, int currentHostGroupCount, int scalingAdjument, int expectedScaleUpCount) {
        MockitoAnnotations.initMocks(this);
        Cluster cluster = getARunningCluster();

        when(scalingEventMock.getAlert()).thenReturn(baseAlertMock);
        when(scalingEventMock.getAlert().getAlertType()).thenReturn(AlertType.LOAD);
        when(scalingEventMock.getExistingClusterNodeCount()).thenReturn(currentClusterNodeCount);
        when(scalingEventMock.getExistingHostGroupNodeCount()).thenReturn(currentHostGroupCount);
        when(scalingEventMock.getDesiredAbsoluteHostGroupNodeCount()).thenReturn(currentHostGroupCount + scalingAdjument);
        when(scalingEventMock.getExistingServiceHealthyHostGroupNodeCount()).thenReturn(currentHostGroupCount);
        when(scalingEventMock.getScalingAdjustmentType()).thenReturn(ScalingAdjustmentType.REGULAR);
        when(scalingEventMock.getActivityId()).thenReturn(1234L);

        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(baseAlertMock.getCluster()).thenReturn(cluster);
        when(baseAlertMock.getScalingPolicy()).thenReturn(scalingPolicyMock);

        when(scalingEventMock.getDecommissionNodeIds()).thenCallRealMethod();
        when(applicationContext.getBean("ScalingRequest", cluster, scalingPolicyMock,
                currentClusterNodeCount, currentHostGroupCount, expectedScaleUpCount, List.of(), currentHostGroupCount, ScalingAdjustmentType.REGULAR, 1234L))
                .thenReturn(runnableMock);

        underTest.onApplicationEvent(scalingEventMock);

        verify(rejectedThreadService).remove(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setLastScalingActivity(eq(AUTOSCALE_CLUSTER_ID), anyLong());
        verify(executorService).submit(runnableMock);
    }

    private Cluster getARunningCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setState(ClusterState.RUNNING);
        cluster.setLastScalingActivity(Instant.now()
                .minus(45, ChronoUnit.MINUTES).toEpochMilli());

        ClusterPertain clusterPertain = new ClusterPertain();
        cluster.setClusterPertain(clusterPertain);
        return cluster;
    }
}
