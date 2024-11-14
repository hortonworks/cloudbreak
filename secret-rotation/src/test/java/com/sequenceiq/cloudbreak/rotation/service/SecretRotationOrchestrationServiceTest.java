package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
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
    private SecretRotationStepProgressService secretRotationStepProgressService;

    @Mock
    private SecretRotationStatusService secretRotationStatusService;

    @Mock
    private SecretRotationUsageService secretRotationUsageService;

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
    private TransactionService transactionService;

    @InjectMocks
    private SecretRotationOrchestrationService underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "rotationContextProviderMap", Map.of(TEST, contextProvider), true);
    }

    @Test
    public void testRotateWhenNotNeeded() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.FALSE);
        underTest.rotateIfNeeded(TEST, RESOURCE, null, null);

        verifyNoInteractions(contextProvider, rotationService, secretRotationStatusService, secretRotationUsageService);
    }

    @Test
    public void testPreValidateWhenNotNeeded() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.FALSE);
        underTest.preValidateIfNeeded(TEST, RESOURCE, null, null);

        verifyNoInteractions(contextProvider, preValidateService, secretRotationStatusService, secretRotationUsageService);
    }

    @Test
    public void testFinalizeWhenNotNeeded() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.FALSE);
        underTest.finalizeIfNeeded(TEST, RESOURCE, null, null);

        verifyNoInteractions(contextProvider, finalizeService, secretRotationStatusService, secretRotationUsageService);
    }

    @Test
    public void testRollbackWhenNotNeeded() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.FALSE);
        underTest.rollbackIfNeeded(TEST, RESOURCE, null, null, new SecretRotationException(ROLLBACK_REASON));

        verifyNoInteractions(contextProvider, rollbackService, secretRotationStatusService, secretRotationUsageService);
    }

    @Test
    public void testRotate() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.TRUE);
        doNothing().when(rotationService).rotate(any());
        underTest.rotateIfNeeded(TEST, RESOURCE, null, null);

        verify(rotationService).rotate(any());
        verify(secretRotationStatusService, times(1)).rotationStarted(eq(RESOURCE), eq(TEST));
        verify(secretRotationUsageService, times(1)).rotationStarted(eq(TEST), eq(RESOURCE), isNull());
        verify(secretRotationStatusService, times(1)).rotationFinished(eq(RESOURCE), eq(TEST));
    }

    @Test
    public void testPreValidate() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.TRUE);
        doNothing().when(preValidateService).preValidate(any());
        underTest.preValidateIfNeeded(TEST, RESOURCE, null, null);

        verify(preValidateService).preValidate(any());
        verifyNoInteractions(secretRotationUsageService, secretRotationStatusService);
    }

    @Test
    public void testFinalize() throws TransactionService.TransactionExecutionException {
        doAnswer((Answer<Void>) invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.TRUE);
        doNothing().when(finalizeService).finalize(any());
        doNothing().when(secretRotationStepProgressService).deleteCurrentRotation(any());

        underTest.finalizeIfNeeded(TEST, RESOURCE, null, null);

        verify(finalizeService).finalize(any());
        verify(secretRotationStatusService, times(1)).finalizeStarted(eq(RESOURCE), eq(TEST));
        verify(secretRotationStatusService, times(1)).finalizeFinished(eq(RESOURCE), eq(TEST));
        verify(secretRotationUsageService, times(1)).rotationFinished(eq(TEST), eq(RESOURCE), isNull());
    }

    @Test
    public void testRollback() {
        when(decisionProvider.executionRequired(any())).thenReturn(Boolean.TRUE);
        doNothing().when(rollbackService).rollback(any());

        underTest.rollbackIfNeeded(TEST, RESOURCE, null, null, new SecretRotationException(ROLLBACK_REASON));

        verify(rollbackService).rollback(any());
        verify(secretRotationStatusService, times(1)).rollbackStarted(eq(RESOURCE), eq(TEST), eq(ROLLBACK_REASON));
        verify(secretRotationUsageService, times(1)).rollbackStarted(eq(TEST), eq(RESOURCE), isNull());
        verify(secretRotationStatusService, times(1)).rollbackFinished(eq(RESOURCE), eq(TEST));
        verify(secretRotationUsageService, times(1)).rollbackFinished(eq(TEST), eq(RESOURCE), isNull());
        verify(secretRotationUsageService, times(1)).rotationFailed(eq(TEST), eq(RESOURCE), eq(ROLLBACK_REASON), isNull());
    }
}
