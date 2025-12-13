package com.sequenceiq.cloudbreak.reactor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncRequest;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.InstanceMetadataUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.status.ClusterStatusUpdater;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.template.ClusterManagerTemplateSyncService;

@ExtendWith(MockitoExtension.class)
class ClusterSyncHandlerTest {

    @InjectMocks
    private ClusterSyncHandler underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterStatusUpdater clusterStatusUpdater;

    @Mock
    private EventBus eventBus;

    @Mock
    private InstanceMetadataUpdater instanceMetadataUpdater;

    @Mock
    private UserDataService userDataService;

    @Mock
    private ClusterManagerTemplateSyncService clusterManagerTemplateSyncService;

    @Test
    void testAcceptWhenStackIsAvailableAndVerifyServiceCalls() throws Exception {
        long stackId = 1L;
        ClusterSyncRequest clusterSyncRequestEvent = mock(ClusterSyncRequest.class);
        Stack stack = mock(Stack.class);
        Cluster cluster = mock(Cluster.class);
        when(clusterSyncRequestEvent.getResourceId()).thenReturn(stackId);
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(clusterService.retrieveClusterByStackIdWithoutAuth(anyLong())).thenReturn(Optional.of(cluster));
        when(stack.isAvailable()).thenReturn(true);
        when(stack.getId()).thenReturn(stackId);

        underTest.accept(new Event<>(clusterSyncRequestEvent));

        verify(clusterStatusUpdater).updateClusterStatus(stack, cluster);
        verify(instanceMetadataUpdater).updatePackageVersionsOnAllInstances(stackId);
        verify(userDataService).makeSureUserDataIsMigrated(stackId);
        verify(clusterManagerTemplateSyncService).sync(stackId);
        verify(eventBus).notify(anyString(), any());
    }

    @Test
    void testAcceptWhenStackIsInMaintenanceModeAndVerifyServiceCalls() throws Exception {
        long stackId = 1L;
        ClusterSyncRequest clusterSyncRequestEvent = mock(ClusterSyncRequest.class);
        Stack stack = mock(Stack.class);
        Cluster cluster = mock(Cluster.class);
        when(clusterSyncRequestEvent.getResourceId()).thenReturn(stackId);
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(clusterService.retrieveClusterByStackIdWithoutAuth(anyLong())).thenReturn(Optional.of(cluster));
        when(stack.isMaintenanceModeEnabled()).thenReturn(true);
        when(stack.getId()).thenReturn(stackId);

        underTest.accept(new Event<>(clusterSyncRequestEvent));

        verify(clusterStatusUpdater).updateClusterStatus(stack, cluster);
        verify(instanceMetadataUpdater).updatePackageVersionsOnAllInstances(stackId);
        verify(userDataService).makeSureUserDataIsMigrated(stackId);
        verify(clusterManagerTemplateSyncService).sync(stackId);
        verify(eventBus).notify(anyString(), any());
    }

    @Test
    void testAcceptWhenClusterIsEmptyAndVerifyServiceCalls() throws Exception {
        long stackId = 1L;
        ClusterSyncRequest clusterSyncRequestEvent = mock(ClusterSyncRequest.class);
        Stack stack = mock(Stack.class);
        when(clusterSyncRequestEvent.getResourceId()).thenReturn(stackId);
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(clusterService.retrieveClusterByStackIdWithoutAuth(anyLong())).thenReturn(Optional.empty());

        underTest.accept(new Event<>(clusterSyncRequestEvent));

        verify(clusterStatusUpdater).updateClusterStatus(any(), any());
        verifyNoInteractions(instanceMetadataUpdater);
        verifyNoInteractions(userDataService);
        verifyNoInteractions(clusterManagerTemplateSyncService);
        verify(eventBus).notify(anyString(), any());
    }

    @Test
    void testAcceptWhenLastServiceCallThrowException() throws Exception {
        long stackId = 1L;
        ClusterSyncRequest clusterSyncRequestEvent = mock(ClusterSyncRequest.class);
        Stack stack = mock(Stack.class);
        Cluster cluster = mock(Cluster.class);
        when(clusterSyncRequestEvent.getResourceId()).thenReturn(stackId);
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(clusterService.retrieveClusterByStackIdWithoutAuth(anyLong())).thenReturn(Optional.of(cluster));
        when(stack.isMaintenanceModeEnabled()).thenReturn(true);
        when(stack.getId()).thenReturn(stackId);
        doThrow(new RuntimeException("Uh-oh, something bad happened!")).when(clusterManagerTemplateSyncService).sync(stackId);

        underTest.accept(new Event<>(clusterSyncRequestEvent));

        verify(clusterStatusUpdater).updateClusterStatus(stack, cluster);
        verify(instanceMetadataUpdater).updatePackageVersionsOnAllInstances(stackId);
        verify(userDataService).makeSureUserDataIsMigrated(stackId);
        verify(clusterManagerTemplateSyncService).sync(stackId);
        verify(eventBus).notify(anyString(), any());
    }
}