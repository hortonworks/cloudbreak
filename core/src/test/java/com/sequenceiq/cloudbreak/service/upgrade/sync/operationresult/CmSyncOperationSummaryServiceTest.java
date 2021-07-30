package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CmSyncOperationSummaryServiceTest {

    @Mock
    private CmSyncOperationResultEvaluatorService cmSyncOperationResultEvaluatorService;

    @InjectMocks
    private CmSyncOperationSummaryService underTest;

    @Test
    void evaluateWhenResultEmptyThenMessageCmIsDown() {
        CmSyncOperationResult cmSyncOperationResult = new CmSyncOperationResult(null, null);

        CmSyncOperationSummary cmSyncOperationSummary = underTest.evaluate(cmSyncOperationResult);

        assertFalse(cmSyncOperationSummary.hasSucceeded());
        assertEquals("CM sync could not be carried out, most probably the CM server is down. Please make sure the CM server is running.",
                cmSyncOperationSummary.getMessage());
        verify(cmSyncOperationResultEvaluatorService, never()).evaluateParcelSync(any(), any());
        verify(cmSyncOperationResultEvaluatorService, never()).evaluateCmRepoSync(any(), any());
    }

    @Test
    void evaluateWhenCmSyncOperationResultHasValuesThenEvaluatorsAreCalled() {
        CmSyncOperationResult cmSyncOperationResult = mock(CmSyncOperationResult.class);
        Optional<CmRepoSyncOperationResult> cmRepoSyncOperationResultOpt = Optional.of(new CmRepoSyncOperationResult(null, null));
        Optional<CmParcelSyncOperationResult> cmParcelSyncOperationResultOpt = Optional.of(new CmParcelSyncOperationResult(Set.of(), Set.of()));
        when(cmSyncOperationResult.getCmRepoSyncOperationResult()).thenReturn(cmRepoSyncOperationResultOpt);
        when(cmSyncOperationResult.getCmParcelSyncOperationResult()).thenReturn(cmParcelSyncOperationResultOpt);

        underTest.evaluate(cmSyncOperationResult);

        verify(cmSyncOperationResultEvaluatorService).evaluateCmRepoSync(eq(cmRepoSyncOperationResultOpt), any());
        verify(cmSyncOperationResultEvaluatorService).evaluateParcelSync(eq(cmParcelSyncOperationResultOpt), any());
    }

}
