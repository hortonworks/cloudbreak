package com.sequenceiq.cloudbreak.websocket;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(MockitoJUnitRunner.class)
public class UptimeNotifierTest {

    @InjectMocks
    private UptimeNotifier underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackService stackService;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private StackUtil stackUtil;

    @Before
    public void init() {
        when(stackUtil.getUptimeForCluster(any(Cluster.class), anyBoolean())).thenReturn(1L);
    }

    @Test
    public void notificationSendingWhenEverythingWorkFine() {
        doNothing().when(notificationSender).send(any(Notification.class));
        List<Cluster> clusters = TestUtil.generateCluster(1);

        when(clusterService.findByStatuses(any())).thenReturn(Collections.singletonList(clusters.get(0)));
        Stack stack1 = TestUtil.stack();
        when(stackService.getForCluster(anyLong())).thenReturn(stack1);

        underTest.sendUptime();

        ArgumentCaptor<Notification<CloudbreakEventsJson>> argument1 = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender).send(argument1.capture());
        Notification<CloudbreakEventsJson> notification = argument1.getValue();
        assertEquals(GCP, notification.getNotification().getCloud());
        assertEquals("null", notification.getNotification().getBlueprintName());
        assertNull(notification.getNotification().getBlueprintId());

        verify(notificationSender, times(1)).send(any(Notification.class));
    }

    @Test
    public void notificationSendingWhenBlueprintNotNullEverythingWorkFine() {
        doNothing().when(notificationSender).send(any(Notification.class));
        List<Cluster> clusters = TestUtil.generateCluster(1);

        when(clusterService.findByStatuses(any())).thenReturn(Collections.singletonList(clusters.get(0)));

        Stack stack2 = TestUtil.stack();
        stack2.setCluster(clusters.get(0));
        when(stackService.getForCluster(anyLong())).thenReturn(stack2);

        underTest.sendUptime();

        ArgumentCaptor<Notification<CloudbreakEventsJson>> argument2 = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender).send(argument2.capture());
        Notification<CloudbreakEventsJson> notification = argument2.getValue();
        assertEquals(GCP, notification.getNotification().getCloud());
        assertEquals("multi-node-yarn", notification.getNotification().getBlueprintName());
        assertEquals(Long.valueOf(1L), notification.getNotification().getBlueprintId());

        verify(notificationSender, times(1)).send(any(Notification.class));
    }

    @Test
    public void notificationSendingWhenCredentialNullEverythingWorkFine() {
        doNothing().when(notificationSender).send(any(Notification.class));
        List<Cluster> clusters = TestUtil.generateCluster(1);

        when(clusterService.findByStatuses(any())).thenReturn(Collections.singletonList(clusters.get(0)));

        Stack stack2 = TestUtil.stack();
        stack2.setCluster(clusters.get(0));
        stack2.setCredential(null);
        when(stackService.getForCluster(anyLong())).thenReturn(stack2);

        underTest.sendUptime();

        ArgumentCaptor<Notification<CloudbreakEventsJson>> argument2 = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender).send(argument2.capture());
        Notification<CloudbreakEventsJson> notification = argument2.getValue();
        assertEquals("null", notification.getNotification().getCloud());
        assertEquals("multi-node-yarn", notification.getNotification().getBlueprintName());
        assertEquals(Long.valueOf(1L), notification.getNotification().getBlueprintId());

        verify(notificationSender, times(1)).send(any(Notification.class));
    }
}
