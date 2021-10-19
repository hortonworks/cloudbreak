package com.sequenceiq.periscope.monitor.handler;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.IntStream;

import javax.ws.rs.ForbiddenException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

public class UpdateFailedHandlerTest {

    private static final long AUTOSCALE_CLUSTER_ID = 1L;

    private static final String CLOUDBREAK_STACK_CRN = "someCrn";

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
    private AltusMachineUserService altusMachineUserService;

    @InjectMocks
    private UpdateFailedHandler underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

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
    }

    @Test
    public void testOnApplicationEventWhenFailsFourTimesWithForbiddenError() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);

        UpdateFailedEvent failedEvent = new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID, new ForbiddenException());
        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(failedEvent));

        verify(clusterService, times(4)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(altusMachineUserService, never()).initializeMachineUserForEnvironment(cluster);
    }

    @Test
    public void testOnApplicationEventWhenFailsFiveTimesWithForbiddenError() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(messagesService.getMessage(anyString())).thenReturn("trigger failed");

        UpdateFailedEvent failedEvent = new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID, new ForbiddenException());
        IntStream.range(0, 5).forEach(i -> underTest.onApplicationEvent(failedEvent));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(historyService).createEntry(eq(ScalingStatus.TRIGGER_FAILED), eq("trigger failed"), eq(cluster));
        verify(altusMachineUserService, times(1)).initializeMachineUserForEnvironment(cluster);
    }

    @Test
    public void testOnApplicationEventWhenFailsFiveTimes() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(messagesService.getMessage(anyString())).thenReturn("trigger failed");

        IntStream.range(0, 5).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(historyService).createEntry(eq(ScalingStatus.TRIGGER_FAILED), eq("trigger failed"), eq(cluster));
    }

    private Cluster getARunningCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(ClusterState.RUNNING);

        ClusterPertain clusterPertain = new ClusterPertain();
        cluster.setClusterPertain(clusterPertain);
        return cluster;
    }
}
