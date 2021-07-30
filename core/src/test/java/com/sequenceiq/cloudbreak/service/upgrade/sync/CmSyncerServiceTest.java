package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.StackComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmParcelSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmRepoSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummaryService;

@ExtendWith(MockitoExtension.class)
public class CmSyncerServiceTest {

    @Mock
    private StackComponentUpdater stackComponentUpdater;

    @Mock
    private ClusterComponentUpdater clusterComponentUpdater;

    @Mock
    private CmInstalledComponentFinderService cmInstalledComponentFinderService;

    @Mock
    private CmServerQueryService cmServerQueryService;

    @Mock
    private CmSyncOperationSummaryService cmSyncOperationSummaryService;

    @Mock
    private CmSyncResultMergerService cmSyncResultMergerService;

    @InjectMocks
    private CmSyncerService underTest;

    @Mock
    private Stack stack;

    @Test
    void testSyncFromCmToDbWhenCmServerRunningThenSyncIsExecuted() {
        when(cmServerQueryService.isCmServerRunning(stack)).thenReturn(true);
        Set<Image> candidateImages = Set.of(mock(Image.class));
        CmRepoSyncOperationResult cmRepoSyncOperationResult = mock(CmRepoSyncOperationResult.class);
        CmParcelSyncOperationResult cmParcelSyncOperationResult = mock(CmParcelSyncOperationResult.class);
        Set<Component> syncedFromServer = Set.of(mock(Component.class));
        when(cmInstalledComponentFinderService.findCmRepoComponent(stack, candidateImages)).thenReturn(cmRepoSyncOperationResult);
        when(cmInstalledComponentFinderService.findParcelComponents(stack, candidateImages)).thenReturn(cmParcelSyncOperationResult);
        when(cmSyncResultMergerService.merge(cmRepoSyncOperationResult, cmParcelSyncOperationResult, stack)).thenReturn(syncedFromServer);

        underTest.syncFromCmToDb(stack, candidateImages);

        verify(cmInstalledComponentFinderService).findCmRepoComponent(eq(stack), eq(candidateImages));
        verify(cmInstalledComponentFinderService).findParcelComponents(eq(stack), eq(candidateImages));
        verify(cmSyncOperationSummaryService).evaluate(any());
        verify(cmSyncResultMergerService).merge(cmRepoSyncOperationResult, cmParcelSyncOperationResult, stack);
        verify(stackComponentUpdater).updateComponentsByStackId(stack, syncedFromServer, false);
        verify(clusterComponentUpdater).updateClusterComponentsByStackId(stack, syncedFromServer, false);
    }

    @Test
    void testSyncFromCmToDbWhenCmServerDownThenSyncSkipped() {
        when(cmServerQueryService.isCmServerRunning(stack)).thenReturn(false);
        Set<Image> candidateImages = Set.of(mock(Image.class));

        CmSyncOperationSummary cmSyncOperationSummary = underTest.syncFromCmToDb(stack, candidateImages);

        assertFalse(cmSyncOperationSummary.hasSucceeded());
        assertEquals("CM server is down, it is not possible to sync parcels and CM version from the server.", cmSyncOperationSummary.getMessage());
        verify(cmServerQueryService).isCmServerRunning(eq(stack));
        verify(cmInstalledComponentFinderService, never()).findCmRepoComponent(any(), any());
        verify(cmInstalledComponentFinderService, never()).findParcelComponents(any(), any());
        verify(cmSyncResultMergerService, never()).merge(any(), any(), eq(stack));
        verify(stackComponentUpdater, never()).updateComponentsByStackId(any(), any(), anyBoolean());
        verify(clusterComponentUpdater, never()).updateClusterComponentsByStackId(any(), any(), anyBoolean());
        verify(cmSyncOperationSummaryService, never()).evaluate(any());
    }

    @Test
    void testSyncFromCmToDbWhenNoCandidateImagesThenSyncSkipped() {
        when(cmServerQueryService.isCmServerRunning(stack)).thenReturn(true);
        Set<Image> candidateImages = Set.of();

        CmSyncOperationSummary cmSyncOperationSummary = underTest.syncFromCmToDb(stack, candidateImages);

        assertFalse(cmSyncOperationSummary.hasSucceeded());
        assertEquals("No candidate images supplied for CM sync, it is not possible to sync parcels and CM version from the server. Please call Cloudera support",
                cmSyncOperationSummary.getMessage());
        verify(cmServerQueryService).isCmServerRunning(eq(stack));
        verify(cmInstalledComponentFinderService, never()).findCmRepoComponent(any(), any());
        verify(cmInstalledComponentFinderService, never()).findParcelComponents(any(), any());
        verify(stackComponentUpdater, never()).updateComponentsByStackId(any(), any(), anyBoolean());
        verify(clusterComponentUpdater, never()).updateClusterComponentsByStackId(any(), any(), anyBoolean());
        verify(cmSyncResultMergerService, never()).merge(any(), any(), eq(stack));
        verify(cmSyncOperationSummaryService, never()).evaluate(any());
    }

}
