package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.common.TestRotationContext;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.cloudbreak.rotation.service.usage.SecretRotationUsageService;

@ExtendWith(MockitoExtension.class)
public class SecretRotationServiceTest {

    @Mock
    private AbstractRotationExecutor executor;

    @Mock
    private RotationContextProvider contextProvider;

    @Mock
    private SecretRotationStepProgressService secretRotationStepProgressService;

    @Mock
    private SecretRotationStatusService secretRotationStatusService;

    @Mock
    private SecretRotationUsageService secretRotationUsageService;

    @InjectMocks
    private SecretRotationService underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeDeclaredField(underTest, "rotationExecutorMap",
                Map.of(STEP, executor), true);
        FieldUtils.writeDeclaredField(underTest, "rotationContextProviderMap",
                Map.of(TestSecretType.TEST, contextProvider), true);
        generateTestContexts();
    }

    @Test
    public void testRotateWhenNotNeeded() {
        underTest.executeRotationIfNeeded(TestSecretType.TEST, "resource", RotationFlowExecutionType.FINALIZE);

        verifyNoInteractions(executor, contextProvider, secretRotationStatusService, secretRotationUsageService);
    }

    @Test
    public void testRotateWhenContextMissing() {
        when(contextProvider.getContexts(anyString())).thenReturn(Map.of());

        assertThrows(RuntimeException.class, () ->
                underTest.executeRotationIfNeeded(TestSecretType.TEST, "resource", null));

        verify(contextProvider).getContexts(anyString());
        verify(secretRotationStatusService, times(1)).rotationStarted(eq("resource"), eq(TestSecretType.TEST));
        verify(secretRotationUsageService, times(1)).rotationStarted(eq(TestSecretType.TEST), eq("resource"), isNull());
        verify(secretRotationStatusService, never()).rotationFinished(anyString(), any());
        verifyNoInteractions(executor);
    }

    @Test
    public void testRotate() {
        doNothing().when(executor).executeRotate(any(), any());

        underTest.executeRotationIfNeeded(TestSecretType.TEST, "resource", null);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executeRotate(any(), any());
        verify(secretRotationStatusService, times(1)).rotationStarted(eq("resource"), eq(TestSecretType.TEST));
        verify(secretRotationUsageService, times(1)).rotationStarted(eq(TestSecretType.TEST), eq("resource"), isNull());
        verify(secretRotationStatusService, times(1)).rotationFinished(eq("resource"), eq(TestSecretType.TEST));
    }

    @Test
    public void testPreValidate() {
        doNothing().when(executor).executePreValidation(any());

        underTest.executePreValidationIfNeeded(TestSecretType.TEST, "resource", null);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executePreValidation(any());
        verifyNoInteractions(secretRotationUsageService, secretRotationStatusService);
    }

    @Test
    public void testFinalize() {
        doNothing().when(secretRotationStepProgressService).deleteAll(any(), any());
        doNothing().when(executor).executeFinalize(any(), any());

        underTest.finalizeRotationIfNeeded(TestSecretType.TEST, "resource", null);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executeFinalize(any(), any());
        verify(secretRotationStatusService, times(1)).finalizeStarted(eq("resource"), eq(TestSecretType.TEST));
        verify(secretRotationStatusService, times(1)).finalizeFinished(eq("resource"), eq(TestSecretType.TEST));
        verify(secretRotationUsageService, times(1)).rotationFinished(eq(TestSecretType.TEST), eq("resource"), isNull());
    }

    @Test
    public void testFinalizeIfPostValidateFails() {
        doThrow(new SecretRotationException("anything", null)).when(executor).executePostValidation(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.finalizeRotationIfNeeded(TestSecretType.TEST, "resource", null));

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(0)).executeFinalize(any(), any());
        verify(executor, times(1)).executePostValidation(any());
        verify(secretRotationStatusService, times(1)).finalizeStarted(eq("resource"), eq(TestSecretType.TEST));
        verify(secretRotationStatusService, never()).finalizeFinished(anyString(), any());
        verify(secretRotationUsageService, never()).rotationFinished(any(), anyString(), isNull());
        verify(secretRotationStatusService, times(1)).finalizeFailed(eq("resource"), eq(TestSecretType.TEST), eq("anything"));
    }

    @Test
    public void testRollback() {
        doNothing().when(executor).executeRollback(any(), any());

        underTest.rollbackRotationIfNeeded(TestSecretType.TEST, "resource", null, STEP);

        verify(contextProvider).getContexts(anyString());
        verify(executor).executeRollback(any(), any());
        verify(secretRotationStatusService, times(1)).rollbackStarted(eq("resource"), eq(TestSecretType.TEST));
        verify(secretRotationUsageService, times(1)).rollbackStarted(eq(TestSecretType.TEST), eq("resource"), isNull());
        verify(secretRotationStatusService, times(1)).rollbackFinished(eq("resource"), eq(TestSecretType.TEST));
        verify(secretRotationUsageService, times(1)).rollbackFinished(eq(TestSecretType.TEST), eq("resource"), isNull());
    }

    private void generateTestContexts() {
        Map<SecretRotationStep, RotationContext> contextMap = Map.of(
                STEP, new TestRotationContext("resource"));
        lenient().when(contextProvider.getContexts(any())).thenReturn(contextMap);
    }
}
