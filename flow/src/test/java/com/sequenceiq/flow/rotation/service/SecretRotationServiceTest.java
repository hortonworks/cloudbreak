package com.sequenceiq.flow.rotation.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;

@ExtendWith(MockitoExtension.class)
public class SecretRotationServiceTest {

    @Mock
    private RotationExecutor<? extends RotationContext> rotationExecutor;

    @Mock
    private RotationContextProvider rotationContextProvider;

    @InjectMocks
    private SecretRotationService underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeDeclaredField(underTest, "rotationExecutorMap",
                Map.of(SecretRotationStep.VAULT, rotationExecutor,
                        SecretRotationStep.CM_USER, rotationExecutor,
                        SecretRotationStep.CLUSTER_PROXY, rotationExecutor), true);
        FieldUtils.writeDeclaredField(underTest, "rotationContextProviderMap",
                Map.of(CloudbreakSecretType.CLOUDBREAK_CM_ADMIN_PASSWORD, rotationContextProvider), true);
    }

    @Test
    public void testRotateWhenNotNeeded() {
        underTest.executeRotation(CloudbreakSecretType.CLOUDBREAK_CM_ADMIN_PASSWORD, "resource", RotationFlowExecutionType.FINALIZE);

        verifyNoInteractions(rotationExecutor, rotationContextProvider);
    }

    @Test
    public void testRotateWhenContextMissing() {
        when(rotationContextProvider.getContexts(anyString())).thenReturn(Map.of());

        assertThrows(RuntimeException.class, () ->
                underTest.executeRotation(CloudbreakSecretType.CLOUDBREAK_CM_ADMIN_PASSWORD, "resource", null));

        verify(rotationContextProvider).getContexts(anyString());
        verifyNoInteractions(rotationExecutor);
    }

    @Test
    public void testRotate() {
        generateTestContexts();
        doNothing().when(rotationExecutor).executeRotate(any());

        underTest.executeRotation(CloudbreakSecretType.CLOUDBREAK_CM_ADMIN_PASSWORD, "resource", null);

        verify(rotationContextProvider).getContexts(anyString());
        verify(rotationExecutor, times(3)).executeRotate(any());
    }

    @Test
    public void testFinalize() {
        generateTestContexts();
        doNothing().when(rotationExecutor).executeFinalize(any());

        underTest.finalizeRotation(CloudbreakSecretType.CLOUDBREAK_CM_ADMIN_PASSWORD, "resource", null);

        verify(rotationContextProvider).getContexts(anyString());
        verify(rotationExecutor, times(3)).executeFinalize(any());
    }

    @Test
    public void testRollback() {
        generateTestContexts();
        doNothing().when(rotationExecutor).executeRollback(any());

        underTest.rollbackRotation(CloudbreakSecretType.CLOUDBREAK_CM_ADMIN_PASSWORD, "resource", null, SecretRotationStep.VAULT);

        verify(rotationContextProvider).getContexts(anyString());
        verify(rotationExecutor).executeRollback(any());
    }

    private void generateTestContexts() {
        Map<SecretRotationStep, RotationContext> contextMap = Map.of(
                SecretRotationStep.VAULT, VaultRotationContext.builder().withResourceCrn("resource").withVaultPathSecretMap(Map.of()).build(),
                SecretRotationStep.CM_USER, new TestRotationContext("resource"),
                SecretRotationStep.CLUSTER_PROXY, new TestRotationContext("resource"));
        when(rotationContextProvider.getContexts(anyString())).thenReturn(contextMap);
    }

    public static class TestRotationContext extends RotationContext {

        protected TestRotationContext(String resourceCrn) {
            super(resourceCrn);
        }
    }
}
