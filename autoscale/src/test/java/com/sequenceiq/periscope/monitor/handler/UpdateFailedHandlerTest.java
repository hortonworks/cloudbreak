package com.sequenceiq.periscope.monitor.handler;

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
import java.util.stream.IntStream;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.domain.UpdateFailedDetails;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.ScalingActivityService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@ExtendWith(MockitoExtension.class)
public class UpdateFailedHandlerTest {

    private static final long AUTOSCALE_CLUSTER_ID = 1L;

    private static final String CLOUDBREAK_STACK_CRN = "someCrn";

    private static final String POLLING_USER = "someOtherCrn";

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @Mock
    private HistoryService historyService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private ScalingActivityService scalingActivityService;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @InjectMocks
    private UpdateFailedHandler underTest;

    @Test
    public void testOnApplicationEventWhenFailsFirstTime() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
    }

    @Test
    public void testOnApplicationEventWhenFailsFourTimes() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));

        verify(clusterService, times(4)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(scalingActivityService, never()).create(any(Cluster.class), any(ActivityStatus.class), anyString(), anyLong());
    }

    @Test
    public void testOnApplicationEventWhenFailsFourTimesWithForbiddenError() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);

        UpdateFailedEvent failedEvent = new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID, new ForbiddenException(), System.currentTimeMillis(), true, POLLING_USER);
        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(failedEvent));

        verify(clusterService, times(4)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, times(4)).setUpdateFailedDetails(eq(AUTOSCALE_CLUSTER_ID), any(UpdateFailedDetails.class));
        verify(clusterService, never()).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(altusMachineUserService, never()).initializeMachineUserForEnvironment(cluster);
        verify(scalingActivityService, never()).create(any(Cluster.class), any(ActivityStatus.class), anyString(), anyLong());
    }

    @Test
    public void testOnApplicationEventWhenFailsFiveTimesWithForbiddenError() {
        Cluster cluster = getARunningCluster();
        ScalingActivity scalingActivity = mock(ScalingActivity.class);

        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(messagesService.getMessageWithArgs(anyString(), any())).thenReturn("metrics collection failed");
        when(scalingActivityService.create(any(Cluster.class), any(ActivityStatus.class), anyString(), anyLong())).thenReturn(scalingActivity);

        UpdateFailedEvent failedEvent = new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID, new ForbiddenException(), System.currentTimeMillis(), true, POLLING_USER);
        IntStream.range(0, 5).forEach(i -> underTest.onApplicationEvent(failedEvent));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(historyService).createEntry(eq(ScalingStatus.TRIGGER_FAILED), eq("metrics collection failed"), eq(cluster));
        verify(scalingActivityService, times(1)).create(any(Cluster.class), eq(ActivityStatus.METRICS_COLLECTION_FAILED),
                eq("metrics collection failed"), anyLong());
        verify(scalingActivityService, times(1)).setEndTime(anyLong(), anyLong());
        verify(clusterService, times(1)).setUpdateFailedDetails(eq(AUTOSCALE_CLUSTER_ID), eq(null));
        verify(altusMachineUserService, times(1)).initializeMachineUserForEnvironment(cluster);
    }

    @Test
    public void testOnApplicationEventWhenFailsFiveTimes() {
        Cluster cluster = getARunningCluster();
        ScalingActivity scalingActivity = mock(ScalingActivity.class);

        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(messagesService.getMessageWithArgs(anyString(), any())).thenReturn("metrics collection failed");
        when(scalingActivityService.create(any(Cluster.class), any(ActivityStatus.class), anyString(), anyLong())).thenReturn(scalingActivity);

        IntStream.range(0, 5).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(historyService).createEntry(eq(ScalingStatus.TRIGGER_FAILED), eq("metrics collection failed"), eq(cluster));
        verify(scalingActivityService, times(1)).create(any(Cluster.class), eq(ActivityStatus.METRICS_COLLECTION_FAILED),
                eq("metrics collection failed"), anyLong());
        verify(scalingActivityService, times(1)).setEndTime(anyLong(), anyLong());
    }

    private Cluster getARunningCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(ClusterState.RUNNING);

        ClusterPertain clusterPertain = new ClusterPertain();
        cluster.setClusterPertain(clusterPertain);

        UpdateFailedDetails updateFailedDetails = new UpdateFailedDetails(Instant.now().minus(45, ChronoUnit.MINUTES).toEpochMilli(),
                3L, true);
        cluster.setUpdateFailedDetails(updateFailedDetails);
        return cluster;
    }
}
