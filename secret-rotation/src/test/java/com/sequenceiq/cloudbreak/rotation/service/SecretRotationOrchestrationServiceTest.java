package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.IGNORE_PREVALIDATE_ERRORS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.history.SecretRotationHistoryService;
import com.sequenceiq.cloudbreak.rotation.service.phase.SecretRotationFinalizeService;
import com.sequenceiq.cloudbreak.rotation.service.phase.SecretRotationPreValidateService;
import com.sequenceiq.cloudbreak.rotation.service.phase.SecretRotationRollbackService;
import com.sequenceiq.cloudbreak.rotation.service.phase.SecretRotationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.cloudbreak.rotation.service.usage.SecretRotationUsageService;

@ExtendWith(MockitoExtension.class)
public class SecretRotationOrchestrationServiceTest {

    private static final String RESOURCE = "resource";

    private static final String ROLLBACK_REASON = "reason";

    @Mock
    private RotationContextProvider contextProvider;

    @Mock
    private SecretRotationStepProgressService stepProgressService;

    @Mock
    private SecretRotationStatusService statusService;

    @Mock
    private SecretRotationUsageService usageService;

    @Mock
    private SecretRotationExecutionDecisionProvider decisionProvider;

    @Mock
    private SecretRotationService rotationService;

    @Mock
    private SecretRotationPreValidateService preValidateService;

    @Mock
    private SecretRotationRollbackService rollbackService;

    @Mock
    private SecretRotationFinalizeService finalizeService;

    @Mock
    private SecretRotationHistoryService historyService;

    @InjectMocks
    private SecretRotationOrchestrationService underTest;

    @Test
    public void testRotateWhenNotNeeded() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.FALSE);
        underTest.rotateIfNeeded(TEST, RESOURCE, null, null);

        verifyNoInteractions(contextProvider, rotationService, statusService, usageService);
    }

    @Test
    public void testPreValidateWhenNotNeeded() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.FALSE);
        assertTrue(underTest.preValidateIfNeeded(TEST, RESOURCE, null, null));

        verifyNoInteractions(contextProvider, preValidateService, statusService, usageService);
    }

    @Test
    public void testFinalizeWhenNotNeeded() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.FALSE);
        underTest.finalizeIfNeeded(TEST, RESOURCE, null, null);

        verifyNoInteractions(contextProvider, finalizeService, statusService, usageService);
    }

    @Test
    public void testRollbackWhenNotNeeded() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.FALSE);
        underTest.rollbackIfNeeded(TEST, RESOURCE, null, null, new SecretRotationException(ROLLBACK_REASON));

        verifyNoInteractions(contextProvider, rollbackService, statusService, usageService);
    }

    @Test
    public void testRotate() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.TRUE);
        doNothing().when(rotationService).rotate(any());
        underTest.rotateIfNeeded(TEST, RESOURCE, null, null);

        verify(rotationService).rotate(any());
        verify(statusService, times(1)).rotationStarted(eq(RESOURCE), eq(TEST));
        verify(usageService, times(1)).rotationStarted(eq(TEST), eq(RESOURCE), isNull());
        verify(statusService, times(1)).rotationFinished(eq(RESOURCE), eq(TEST));
    }

    @Test
    public void testPreValidate() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.TRUE);
        doNothing().when(preValidateService).preValidate(any());
        assertTrue(underTest.preValidateIfNeeded(TEST, RESOURCE, null, null));

        verify(preValidateService).preValidate(any());
        verifyNoInteractions(usageService, statusService);
    }

    @Test
    public void testPreValidateFailureReturnsFalseWhenIgnorePrevalidateErrorsTrueAndDeletesProgress() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.TRUE);
        doThrow(new SecretRotationException("prevalidate failed")).when(preValidateService).preValidate(any());
        doNothing().when(stepProgressService).deleteCurrentRotation(any());

        assertFalse(underTest.preValidateIfNeeded(TEST, RESOURCE, null, Map.of(IGNORE_PREVALIDATE_ERRORS, "true")));

        verify(preValidateService).preValidate(any());
        verify(stepProgressService, times(1)).deleteCurrentRotation(any());
        verifyNoInteractions(usageService, statusService);
    }

    @Test
    public void testPreValidateFailureThrowsWhenIgnorePrevalidateErrorsNotSet() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.TRUE);
        doThrow(new SecretRotationException("prevalidate failed")).when(preValidateService).preValidate(any());
        doNothing().when(stepProgressService).deleteCurrentRotation(any());

        assertThrows(SecretRotationException.class, () -> underTest.preValidateIfNeeded(TEST, RESOURCE, null, null));

        verify(preValidateService).preValidate(any());
        verify(stepProgressService, times(1)).deleteCurrentRotation(any());
        verifyNoInteractions(usageService, statusService);
    }

    @Test
    public void testFinalize() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.TRUE);
        doNothing().when(finalizeService).finalize(any());
        doNothing().when(stepProgressService).deleteCurrentRotation(any());
        doNothing().when(historyService).addHistoryItem(any());

        underTest.finalizeIfNeeded(TEST, RESOURCE, null, null);

        verify(finalizeService).finalize(any());
        verify(statusService, times(1)).finalizeStarted(eq(RESOURCE), eq(TEST));
        verify(statusService, times(1)).finalizeFinished(eq(RESOURCE), eq(TEST));
        verify(usageService, times(1)).rotationFinished(eq(TEST), eq(RESOURCE), isNull());
        verify(historyService, times(1)).addHistoryItem(any());
    }

    @Test
    public void testRollback() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.TRUE);
        doNothing().when(rollbackService).rollback(any());

        underTest.rollbackIfNeeded(TEST, RESOURCE, null, null, new SecretRotationException(ROLLBACK_REASON));

        verify(rollbackService).rollback(any());
        verify(statusService, times(1)).rollbackStarted(eq(RESOURCE), eq(TEST), eq(ROLLBACK_REASON));
        verify(usageService, times(1)).rollbackStarted(eq(TEST), eq(RESOURCE), isNull());
        verify(statusService, times(1)).rollbackFinished(eq(RESOURCE), eq(TEST));
        verify(usageService, times(1)).rollbackFinished(eq(TEST), eq(RESOURCE), isNull());
        verify(usageService, times(1)).rotationFailed(eq(TEST), eq(RESOURCE), eq(ROLLBACK_REASON), isNull());
    }
}
