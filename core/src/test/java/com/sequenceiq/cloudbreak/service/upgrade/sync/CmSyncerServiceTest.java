package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmInstalledComponentFinderService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.db.ComponentPersistingService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmParcelSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmRepoSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationStatus;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummaryService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class CmSyncerServiceTest {

    private static final long STACK_ID = 1L;

    private static final long WORKSPACE_ID = 2L;

    @Mock
    private CmInstalledComponentFinderService cmInstalledComponentFinderService;

    @Mock
    private CmServerQueryService cmServerQueryService;

    @Mock
    private CmSyncOperationSummaryService cmSyncOperationSummaryService;

    @Mock
    private ComponentPersistingService componentPersistingService;

    @InjectMocks
    private CmSyncerService underTest;

    @Mock
    private Stack stack;

    @Mock
    private MixedPackageVersionService mixedPackageVersionService;

    @Test
    void testSyncFromCmToDbWhenCmServerRunningThenSyncIsExecuted() {
        when(cmServerQueryService.isCmServerNotRunning(stack)).thenReturn(false);
        Set<Image> candidateImages = Set.of(mock(Image.class));
        CmRepoSyncOperationResult cmRepoSyncOperationResult = mock(CmRepoSyncOperationResult.class);
        CmParcelSyncOperationResult cmParcelSyncOperationResult = mock(CmParcelSyncOperationResult.class);
        when(stack.getId()).thenReturn(STACK_ID);
        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(cmInstalledComponentFinderService.findCmRepoComponent(stack, candidateImages)).thenReturn(cmRepoSyncOperationResult);
        when(cmInstalledComponentFinderService.findParcelComponents(stack, candidateImages)).thenReturn(cmParcelSyncOperationResult);
        when(cmSyncOperationSummaryService.evaluate(any())).thenReturn(CmSyncOperationStatus.builder().withSuccess("myMessage").build());

        CmSyncOperationSummary cmSyncOperationSummary = underTest.syncFromCmToDb(stack, candidateImages);
        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationSummary.getSyncOperationStatus();

        assertTrue(cmSyncOperationStatus.hasSucceeded());
        assertEquals("myMessage", cmSyncOperationStatus.getMessage());
        verify(cmInstalledComponentFinderService).findCmRepoComponent(stack, candidateImages);
        verify(cmInstalledComponentFinderService).findParcelComponents(stack, candidateImages);
        verify(stack).getId();
        verify(mixedPackageVersionService).validatePackageVersions(eq(WORKSPACE_ID), eq(STACK_ID), any(), eq(candidateImages));
        verifyEvaluateCmSyncResults(cmRepoSyncOperationResult, cmParcelSyncOperationResult);
        verifyPersistComponentsToDb(cmRepoSyncOperationResult, cmParcelSyncOperationResult);
    }

    private void verifyEvaluateCmSyncResults(CmRepoSyncOperationResult cmRepoSyncOperationResult, CmParcelSyncOperationResult cmParcelSyncOperationResult) {
        ArgumentCaptor<CmSyncOperationResult> argumentCaptor = ArgumentCaptor.forClass(CmSyncOperationResult.class);
        verify(cmSyncOperationSummaryService).evaluate(argumentCaptor.capture());
        CmSyncOperationResult cmSyncOperationResult = argumentCaptor.getValue();
        assertEquals(cmRepoSyncOperationResult, cmSyncOperationResult.getCmRepoSyncOperationResult());
        assertEquals(cmParcelSyncOperationResult, cmSyncOperationResult.getCmParcelSyncOperationResult());
    }

    private void verifyPersistComponentsToDb(CmRepoSyncOperationResult cmRepoSyncOperationResult, CmParcelSyncOperationResult cmParcelSyncOperationResult) {
        ArgumentCaptor<CmSyncOperationResult> cmSyncOperationResultArgumentCaptor = ArgumentCaptor.forClass(CmSyncOperationResult.class);
        verify(componentPersistingService).persistComponentsToDb(eq(stack), cmSyncOperationResultArgumentCaptor.capture());
        CmSyncOperationResult cmSyncOperationResult = cmSyncOperationResultArgumentCaptor.getValue();
        assertEquals(cmRepoSyncOperationResult, cmSyncOperationResult.getCmRepoSyncOperationResult());
        assertEquals(cmParcelSyncOperationResult, cmSyncOperationResult.getCmParcelSyncOperationResult());
    }

    @Test
    void testSyncFromCmToDbWhenCmServerDownThenSyncSkipped() {
        when(cmServerQueryService.isCmServerNotRunning(stack)).thenReturn(true);
        Set<Image> candidateImages = Set.of(mock(Image.class));

        CmSyncOperationSummary cmSyncOperationSummary = underTest.syncFromCmToDb(stack, candidateImages);
        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationSummary.getSyncOperationStatus();

        assertFalse(cmSyncOperationStatus.hasSucceeded());
        assertEquals("CM server is down, it is not possible to sync parcels and CM version from the server.", cmSyncOperationStatus.getMessage());
        verify(cmServerQueryService).isCmServerNotRunning(eq(stack));
        verify(cmInstalledComponentFinderService, never()).findCmRepoComponent(any(), any());
        verify(cmInstalledComponentFinderService, never()).findParcelComponents(any(), any());
        verify(cmSyncOperationSummaryService, never()).evaluate(any());
    }

    @Test
    void testSyncFromCmToDbWhenNoCandidateImagesThenSyncSkipped() {
        when(cmServerQueryService.isCmServerNotRunning(stack)).thenReturn(false);
        Set<Image> candidateImages = Set.of();

        CmSyncOperationSummary cmSyncOperationSummary = underTest.syncFromCmToDb(stack, candidateImages);
        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationSummary.getSyncOperationStatus();

        assertFalse(cmSyncOperationStatus.hasSucceeded());
        assertEquals(
                "No candidate images supplied for CM sync, it is not possible to sync parcels and CM version from the server. "
                        + "Please open Cloudera support ticket to investigate the issue",
                cmSyncOperationStatus.getMessage());
        verify(cmServerQueryService).isCmServerNotRunning(eq(stack));
        verify(cmInstalledComponentFinderService, never()).findCmRepoComponent(any(), any());
        verify(cmInstalledComponentFinderService, never()).findParcelComponents(any(), any());
        verify(cmSyncOperationSummaryService, never()).evaluate(any());
    }

}
