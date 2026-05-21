package com.sequenceiq.cloudbreak.job.disk;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskInstanceInfoCollector;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.ResourceSyncUtil;
import com.sequenceiq.cloudbreak.util.StackStatusAndReachabilityValidatorUtil;

@ExtendWith(MockitoExtension.class)
class DiskSyncServiceTest {

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceSyncUtil resourceSyncUtil;

    @Mock
    private StackService stackService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private StackStatusAndReachabilityValidatorUtil stackStatusAndReachabilityValidatorUtil;

    @Mock
    private DiskInstanceInfoCollector diskInstanceInfoCollector;

    @InjectMocks
    private DiskSyncService underTest;

    @Test
    void testSyncResourcesWhenStackIsNotInAValidState() {
        StackDto stackDto = mock(StackDto.class);
        Stack stack = mock(Stack.class);
        when(stackDto.getId()).thenReturn(1L);
        when(stackService.getByIdWithLists(1L)).thenReturn(stack);
        when(stack.getDetailedStatus()).thenReturn(DetailedStackStatus.AVAILABLE);
        when(stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)).thenReturn(false);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.syncResources(stackDto, DiskSyncMode.DRY_RUN));

        verify(eventService).fireCloudbreakEvent(eq(1L), eq("AVAILABLE"), eq(DISK_SYNC_FAILED), anyList());
        assertEquals("Exception while trying to sync disks - The stack is either not in a valid state or not all nodes or reachable for disk sync to run!",
                exception.getMessage());
    }

    @Test
    void testSyncResourcesWhenNoVolumeSetResources() {
        StackDto stackDto = mock(StackDto.class);
        Stack stack = mock(Stack.class);
        when(stackDto.getId()).thenReturn(1L);
        when(stackService.getByIdWithLists(1L)).thenReturn(stack);
        when(stack.getDetailedStatus()).thenReturn(DetailedStackStatus.AVAILABLE);
        when(stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)).thenReturn(true);
        when(stackDto.getCloudPlatform()).thenReturn("AWS");
        when(resourceService.findAllByStackIdAndResourceTypeIn(any(), any())).thenReturn(Collections.emptyList());

        underTest.syncResources(stackDto, DiskSyncMode.DRY_RUN);

        verifyNoInteractions(eventService);
    }

    @Test
    void testSyncResourcesThrowsException() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(1L);
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithLists(1L)).thenReturn(stack);
        when(stack.getDetailedStatus()).thenReturn(DetailedStackStatus.AVAILABLE);
        when(stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)).thenThrow(new RuntimeException("error"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.syncResources(stackDto, DiskSyncMode.DRY_RUN));

        verify(eventService).fireCloudbreakEvent(eq(1L), eq("AVAILABLE"), eq(DISK_SYNC_FAILED), anyList());
        assertEquals("Exception while trying to sync disks - error", exception.getMessage());
    }
}
