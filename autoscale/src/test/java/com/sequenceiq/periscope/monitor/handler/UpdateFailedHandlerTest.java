package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_SUSPENDED;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

public class UpdateFailedHandlerTest {

    private static final long AUTOSCALE_CLUSTER_ID = 1L;

    private static final String CLOUDBREAK_STACK_CRN = "someCrn";

    @Mock
    private ClusterService clusterService;

    @Mock
    private HistoryService historyService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private StackResponseUtils stackResponseUtils;

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
    public void testOnApplicationEventWhenFailsBeyondThreshold() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(messagesService.getMessage(AUTOSCALING_SUSPENDED)).thenReturn("suspended");

        IntStream.range(0, 15).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));

        verify(clusterService, times(15)).findById(AUTOSCALE_CLUSTER_ID);
        verify(historyService).createEntry(ScalingStatus.DISABLED, "suspended", cluster);
        verify(clusterService).setAutoscaleState(AUTOSCALE_CLUSTER_ID, false);
    }

    private Cluster getARunningCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(ClusterState.RUNNING);
        return cluster;
    }
}
