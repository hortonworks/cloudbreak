package com.sequenceiq.cloudbreak.websocket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;

@RunWith(MockitoJUnitRunner.class)
public class UptimeNotifierTest {

    @InjectMocks
    private UptimeNotifier underTest;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private NotificationSender notificationSender;


    @Test
    public void notificationSendingWhenEverythingWorkFine() {
        doNothing().when(notificationSender).send(any(Notification.class));
        List<Cluster> clusters = TestUtil.generateCluster(1);

        when(clusterRepository.findAll()).thenReturn(Arrays.asList(clusters.get(0)));
        Stack stack1 = TestUtil.stack();
        when(stackRepository.findStackForCluster(anyLong())).thenReturn(stack1);

        underTest.sendUptime();

        ArgumentCaptor<Notification> argument1 = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender).send(argument1.capture());
        assertEquals(CloudPlatform.AZURE.name(), argument1.getValue().getCloud());
        assertEquals("null", argument1.getValue().getBlueprintName());
        assertEquals(null, argument1.getValue().getBlueprintId());

        verify(notificationSender, times(1)).send(any(Notification.class));
    }

    @Test
    public void notificationSendingWhenBlueprintNotNullEverythingWorkFine() {
        doNothing().when(notificationSender).send(any(Notification.class));
        List<Cluster> clusters = TestUtil.generateCluster(1);

        when(clusterRepository.findAll()).thenReturn(Arrays.asList(clusters.get(0)));

        Stack stack2 = TestUtil.stack();
        stack2.setCluster(clusters.get(0));
        when(stackRepository.findStackForCluster(anyLong())).thenReturn(stack2);

        underTest.sendUptime();

        ArgumentCaptor<Notification> argument2 = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender).send(argument2.capture());
        assertEquals(CloudPlatform.AZURE.name(), argument2.getValue().getCloud());
        assertEquals("multi-node-yarn", argument2.getValue().getBlueprintName());
        assertEquals(Long.valueOf(1), argument2.getValue().getBlueprintId());

        verify(notificationSender, times(1)).send(any(Notification.class));
    }

    @Test
    public void notificationSendingWhenCredentialNullEverythingWorkFine() {
        doNothing().when(notificationSender).send(any(Notification.class));
        List<Cluster> clusters = TestUtil.generateCluster(1);

        when(clusterRepository.findAll()).thenReturn(Arrays.asList(clusters.get(0)));

        Stack stack2 = TestUtil.stack();
        stack2.setCluster(clusters.get(0));
        stack2.setCredential(null);
        when(stackRepository.findStackForCluster(anyLong())).thenReturn(stack2);

        underTest.sendUptime();

        ArgumentCaptor<Notification> argument2 = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender).send(argument2.capture());
        assertEquals("null", argument2.getValue().getCloud());
        assertEquals("multi-node-yarn", argument2.getValue().getBlueprintName());
        assertEquals(Long.valueOf(1), argument2.getValue().getBlueprintId());

        verify(notificationSender, times(1)).send(any(Notification.class));
    }
}