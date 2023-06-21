package com.sequenceiq.flow.rotation.service;

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

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.flow.rotation.TestRotationContext;
import com.sequenceiq.flow.rotation.TestSecretRotationStep;
import com.sequenceiq.flow.rotation.TestSecretType;

@ExtendWith(MockitoExtension.class)
public class SecretRotationServiceTest {

    @Mock
    private RotationExecutor executor;

    @Mock
    private RotationContextProvider contextProvider;

    @InjectMocks
    private SecretRotationService underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeDeclaredField(underTest, "rotationExecutorMap",
                Map.of(TestSecretRotationStep.TEST_STEP, executor), true);
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
        doNothing().when(executor).executeRotate(any());

        underTest.executeRotation(TestSecretType.TEST, "resource", null);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executeRotate(any());
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
        doNothing().when(executor).executeFinalize(any());

        underTest.finalizeRotation(TestSecretType.TEST, "resource", null);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executeFinalize(any());
    }

    @Test
    public void testFinalizeIfPostValidateFails() {
        doThrow(new SecretRotationException("anything", null)).when(executor).executePostValidation(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.finalizeRotation(TestSecretType.TEST, "resource", null));

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(0)).executeFinalize(any());
        verify(executor, times(1)).executePostValidation(any());
    }

    @Test
    public void testRollback() {
        doNothing().when(executor).executeRollback(any());

        underTest.rollbackRotation(TestSecretType.TEST, "resource", null, TestSecretRotationStep.TEST_STEP);

        verify(contextProvider).getContexts(anyString());
        verify(executor).executeRollback(any());
    }

    private void generateTestContexts() {
        Map<SecretRotationStep, RotationContext> contextMap = Map.of(
                TestSecretRotationStep.TEST_STEP, new TestRotationContext("resource"));
        lenient().when(contextProvider.getContexts(any())).thenReturn(contextMap);
    }
}
