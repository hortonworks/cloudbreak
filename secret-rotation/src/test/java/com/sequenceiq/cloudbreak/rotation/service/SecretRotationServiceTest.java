package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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

@ExtendWith(MockitoExtension.class)
public class SecretRotationServiceTest {

    @Mock
    private AbstractRotationExecutor executor;

    @Mock
    private RotationContextProvider contextProvider;

    @Mock
    private SecretRotationStepProgressService secretRotationStepProgressService;

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
        underTest.executeRotation(TestSecretType.TEST, "resource", RotationFlowExecutionType.FINALIZE);

        verifyNoInteractions(executor, contextProvider);
    }

    @Test
    public void testRotateWhenContextMissing() {
        when(contextProvider.getContexts(anyString())).thenReturn(Map.of());

        assertThrows(RuntimeException.class, () ->
                underTest.executeRotation(TestSecretType.TEST, "resource", null));

        verify(contextProvider).getContexts(anyString());
        verifyNoInteractions(executor);
    }

    @Test
    public void testRotate() {
        doNothing().when(executor).executeRotate(any(), any());

        underTest.executeRotation(TestSecretType.TEST, "resource", null);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executeRotate(any(), any());
    }

    @Test
    public void testPreValidate() {
        doNothing().when(executor).executePreValidation(any());

        underTest.executePreValidation(TestSecretType.TEST, "resource", null);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executePreValidation(any());
    }

    @Test
    public void testFinalize() {
        doNothing().when(secretRotationStepProgressService).deleteAll(any(), any());
        doNothing().when(executor).executeFinalize(any(), any());

        underTest.finalizeRotation(TestSecretType.TEST, "resource", null);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executeFinalize(any(), any());
    }

    @Test
    public void testFinalizeIfPostValidateFails() {
        doThrow(new SecretRotationException("anything", null)).when(executor).executePostValidation(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.finalizeRotation(TestSecretType.TEST, "resource", null));

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(0)).executeFinalize(any(), any());
        verify(executor, times(1)).executePostValidation(any());
    }

    @Test
    public void testRollback() {
        doNothing().when(executor).executeRollback(any(), any());

        underTest.rollbackRotation(TestSecretType.TEST, "resource", null, STEP);

        verify(contextProvider).getContexts(anyString());
        verify(executor).executeRollback(any(), any());
    }

    private void generateTestContexts() {
        Map<SecretRotationStep, RotationContext> contextMap = Map.of(
                STEP, new TestRotationContext("resource"));
        lenient().when(contextProvider.getContexts(any())).thenReturn(contextMap);
    }
}
