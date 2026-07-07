package com.sequenceiq.cloudbreak.job.disk;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskSyncMode;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskInstanceInfoCollector;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.ResourceSyncUtil;
import com.sequenceiq.cloudbreak.util.StackStatusAndReachabilityValidatorUtil;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.ProviderSyncState;

@ExtendWith(MockitoExtension.class)
class DiskSyncServiceTest {

    private static final long STACK_ID = 1L;

    private static final String STACK_CRN = "crn:stack";

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

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private DiskSyncService underTest;

    @Test
    void testSyncResourcesWhenStackIsNotInAValidState() {
        StackDto stackDto = mock(StackDto.class);
        Stack stack = mock(Stack.class);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        when(stack.getDetailedStatus()).thenReturn(DetailedStackStatus.AVAILABLE);
        when(stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)).thenReturn(false);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.syncResources(stackDto, DiskSyncMode.DRY_RUN));

        verify(eventService).fireCloudbreakEvent(eq(STACK_ID), eq("AVAILABLE"), eq(DISK_SYNC_FAILED), anyList());
        assertEquals("Exception while trying to sync disks - The stack is either not in a valid state or not all nodes or reachable for disk sync to run!",
                exception.getMessage());
        verifyNoInteractions(stackUpdater);
    }

    @Test
    void testSyncResourcesWhenNoVolumeSetResources() {
        StackDto stackDto = validStackDto(Set.of());
        when(resourceService.findAllByStackIdAndResourceTypeIn(any(), any())).thenReturn(Collections.emptyList());

        underTest.syncResources(stackDto, DiskSyncMode.DRY_RUN);

        verifyNoInteractions(eventService);
        verify(stackUpdater, never()).addProviderState(any(), any(), any());
        verify(stackUpdater, never()).removeProviderStates(any(), any(), any());
    }

    @Test
    void testDryRunMismatchNotYetReportedFiresWarningAndMarksState() {
        StackDto stackDto = validStackDto(Set.of());
        when(resourceSyncUtil.updateResource(any(), any(), any(), any(), anyBoolean())).thenReturn(true);

        underTest.syncResources(stackDto, DiskSyncMode.DRY_RUN);

        verify(resourceSyncUtil).checkForUnmountedVolumes(any(), any(), any(), any(), eq(false));
        verify(resourceSyncUtil).updateResource(any(), any(), any(), eq(DiskSyncMode.DRY_RUN), eq(false));
        verify(stackUpdater).addProviderState(STACK_CRN, STACK_ID, ProviderSyncState.DISK_MISMATCH_FOUND);
        verify(stackUpdater, never()).removeProviderStates(any(), any(), any());
    }

    @Test
    void testDryRunMismatchAlreadyReportedSuppressesWarningAndIsIdempotent() {
        StackDto stackDto = validStackDto(Set.of(ProviderSyncState.DISK_MISMATCH_FOUND));
        when(resourceSyncUtil.updateResource(any(), any(), any(), any(), anyBoolean())).thenReturn(true);

        underTest.syncResources(stackDto, DiskSyncMode.DRY_RUN);

        verify(resourceSyncUtil).checkForUnmountedVolumes(any(), any(), any(), any(), eq(true));
        verify(resourceSyncUtil).updateResource(any(), any(), any(), eq(DiskSyncMode.DRY_RUN), eq(true));
        verify(stackUpdater, never()).addProviderState(any(), any(), any());
        verify(stackUpdater, never()).removeProviderStates(any(), any(), any());
    }

    @Test
    void testDryRunNoMismatchAlreadyReportedClearsState() {
        StackDto stackDto = validStackDto(Set.of(ProviderSyncState.DISK_MISMATCH_FOUND));

        underTest.syncResources(stackDto, DiskSyncMode.DRY_RUN);

        verify(stackUpdater).removeProviderStates(STACK_CRN, STACK_ID, Set.of(ProviderSyncState.DISK_MISMATCH_FOUND));
        verify(stackUpdater, never()).addProviderState(any(), any(), any());
    }

    @Test
    void testDryRunNoMismatchNotReportedDoesNotChangeState() {
        StackDto stackDto = validStackDto(Set.of());

        underTest.syncResources(stackDto, DiskSyncMode.DRY_RUN);

        verify(stackUpdater, never()).addProviderState(any(), any(), any());
        verify(stackUpdater, never()).removeProviderStates(any(), any(), any());
    }

    @Test
    void testPersistClearsStateRegardlessOfMismatch() {
        StackDto stackDto = validStackDto(Set.of(ProviderSyncState.DISK_MISMATCH_FOUND));
        when(resourceSyncUtil.updateResource(any(), any(), any(), any(), anyBoolean())).thenReturn(true);

        underTest.syncResources(stackDto, DiskSyncMode.PERSIST);

        verify(stackUpdater).removeProviderStates(STACK_CRN, STACK_ID, Set.of(ProviderSyncState.DISK_MISMATCH_FOUND));
        verify(stackUpdater, never()).addProviderState(any(), any(), any());
    }

    @Test
    void testSyncResourcesThrowsException() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(STACK_ID);
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        when(stack.getDetailedStatus()).thenReturn(DetailedStackStatus.AVAILABLE);
        when(stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)).thenThrow(new RuntimeException("error"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.syncResources(stackDto, DiskSyncMode.DRY_RUN));

        verify(eventService).fireCloudbreakEvent(eq(STACK_ID), eq("AVAILABLE"), eq(DISK_SYNC_FAILED), anyList());
        assertEquals("Exception while trying to sync disks - error", exception.getMessage());
        verifyNoInteractions(stackUpdater);
    }

    private StackDto validStackDto(Set<ProviderSyncState> providerSyncStates) {
        StackDto stackDto = mock(StackDto.class);
        Stack stack = mock(Stack.class);
        StackView stackView = mock(StackView.class);
        when(stackDto.getId()).thenReturn(STACK_ID);
        lenient().when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        when(stackDto.getCloudPlatform()).thenReturn("AWS");
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackView.getProviderSyncStates()).thenReturn(providerSyncStates);
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        when(stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)).thenReturn(true);
        return stackDto;
    }
}
