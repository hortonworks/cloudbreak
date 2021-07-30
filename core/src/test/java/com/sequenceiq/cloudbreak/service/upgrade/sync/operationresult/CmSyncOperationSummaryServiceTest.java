package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;

@ExtendWith(MockitoExtension.class)
public class CmSyncOperationSummaryServiceTest {

    @Mock
    private CmSyncOperationResultEvaluatorService cmSyncOperationResultEvaluatorService;

    @InjectMocks
    private CmSyncOperationSummaryService underTest;

    @Test
    void evaluateWhenCmSyncOperationResultHasValuesThenEvaluatorsAreCalled() {
        CmRepoSyncOperationResult cmRepoSyncOperationResult = new CmRepoSyncOperationResult("installedCmVersion", null);
        CmParcelSyncOperationResult cmParcelSyncOperationResult = new CmParcelSyncOperationResult(Set.of(new ParcelInfo("", "")), Set.of());
        CmSyncOperationResult cmSyncOperationResult = new CmSyncOperationResult(cmRepoSyncOperationResult, cmParcelSyncOperationResult);

        CmSyncOperationSummary.Builder cmSyncOperationSummaryBuilder = mock(CmSyncOperationSummary.Builder.class);
        when(cmSyncOperationSummaryBuilder.build()).thenReturn(CmSyncOperationSummary.builder().withSuccess("successStory").build());

        when(cmSyncOperationResultEvaluatorService.evaluateCmRepoSync(cmRepoSyncOperationResult)).thenReturn(cmSyncOperationSummaryBuilder);
        when(cmSyncOperationResultEvaluatorService.evaluateParcelSync(cmParcelSyncOperationResult)).thenReturn(CmSyncOperationSummary.builder());

        CmSyncOperationSummary endResult = underTest.evaluate(cmSyncOperationResult);

        assertTrue(endResult.hasSucceeded());
        assertEquals("successStory", endResult.getMessage());
        verify(cmSyncOperationResultEvaluatorService).evaluateCmRepoSync(cmRepoSyncOperationResult);
        verify(cmSyncOperationResultEvaluatorService).evaluateParcelSync(cmParcelSyncOperationResult);
        verify(cmSyncOperationSummaryBuilder).merge(any());
    }

    @Test
    void evaluateWhenResultEmptyThenMessageCmIsDown() {
        CmRepoSyncOperationResult cmRepoSyncOperationResult = new CmRepoSyncOperationResult(null, null);
        CmParcelSyncOperationResult cmParcelSyncOperationResult = new CmParcelSyncOperationResult(Set.of(), Set.of());
        CmSyncOperationResult cmSyncOperationResult = new CmSyncOperationResult(cmRepoSyncOperationResult, cmParcelSyncOperationResult);

        CmSyncOperationSummary cmSyncOperationSummary = underTest.evaluate(cmSyncOperationResult);

        assertFalse(cmSyncOperationSummary.hasSucceeded());
        assertEquals("CM sync could not be carried out, most probably the CM server is down. Please make sure the CM server is running.",
                cmSyncOperationSummary.getMessage());
        verify(cmSyncOperationResultEvaluatorService, never()).evaluateParcelSync(any());
        verify(cmSyncOperationResultEvaluatorService, never()).evaluateCmRepoSync(any());
    }

}
