package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.rotation.RotationMetadataTestUtil;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.SaltRunOrchestratorStateRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;

@ExtendWith(MockitoExtension.class)
class SaltRunOrchestratorStateRotationExecutorTest {
    @Mock
    private SecretRotationSaltService saltService;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private SaltRunOrchestratorStateRotationExecutor underTest;

    @Captor
    private ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsArgumentCaptor;

    @Test
    public void testPreValidation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(saltService).validateSalt(any(), any());
        doNothing().when(saltService).executeSaltRun(any());

        underTest.executePreValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(saltService).validateSalt(any(), any());
        verify(saltService).executeSaltRun(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("preValidate", orchestratorStateParams.getState());
    }

    @Test
    public void testPreValidationIfvalidateSaltFails() {
        doThrow(new SecretRotationException("validateSaltpong")).when(saltService).validateSalt(any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executePreValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null));

        verify(saltService).validateSalt(any(), any());
        verifyNoMoreInteractions(saltService);
    }

    @Test
    public void testPostValidation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(saltService).executeSaltRun(any());

        underTest.executePostValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(saltService).executeSaltRun(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("postValidate", orchestratorStateParams.getState());
    }

    @Test
    public void testRotation() throws Exception {
        doNothing().when(saltService).executeSaltRun(any());

        underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(saltService).executeSaltRun(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("state", orchestratorStateParams.getState());
    }

    @Test
    public void testRotationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(saltService).executeSaltRun(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()),
                        RotationMetadataTestUtil.metadataForRotation("resource", null)));

        verify(saltService).executeSaltRun(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("state", orchestratorStateParams.getState());
    }

    @Test
    public void testRollbackFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(saltService).executeSaltRun(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()),
                        RotationMetadataTestUtil.metadataForRollback("resource", null)));

        verify(saltService).executeSaltRun(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("rollback", orchestratorStateParams.getState());
    }

    @Test
    public void testRollbackWithOwnStates() throws Exception {
        doNothing().when(saltService).executeSaltRun(any());

        underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()), null);

        verify(saltService).executeSaltRun(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("rollback", orchestratorStateParams.getState());
    }

    @Test
    public void testFinalization() throws Exception {
        doNothing().when(saltService).executeSaltRun(any());

        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))), null);

        verify(saltService).executeSaltRun(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("finalize", orchestratorStateParams.getState());
    }

    @Test
    public void testFinalizationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(saltService).executeSaltRun(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))),
                        RotationMetadataTestUtil.metadataForFinalize("resource", null)));

        verify(saltService).executeSaltRun(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("finalize", orchestratorStateParams.getState());
    }

    @Test
    public void testFinalizationIfNotNeeded() throws Exception {
        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verifyNoInteractions(saltService);
    }

    private SaltRunOrchestratorStateRotationContext createContext(List<String> states, Optional<List<String>> rollbackStates,
            Optional<List<String>> finalizeStates) {
        SaltRunOrchestratorStateRotationContext.SaltRunOrchestratorStateRotationContextBuilder saltStateApplyRotationContextBuilder =
                new SaltRunOrchestratorStateRotationContext.SaltRunOrchestratorStateRotationContextBuilder()
                        .withStates(states).withRotateParams(Map.of());
        rollbackStates.ifPresent(strings -> saltStateApplyRotationContextBuilder.withRollbackStates(strings)
                .withRollbackParams(Map.of()).withRollbackParams(Map.of()));
        finalizeStates.ifPresent(strings -> saltStateApplyRotationContextBuilder.withCleanupStates(strings)
                .withCleanupParams(Map.of()).withCleanupParams(Map.of()));
        saltStateApplyRotationContextBuilder.withPreValidateStates(List.of("preValidate")).withPreValidateParams(Map.of());
        saltStateApplyRotationContextBuilder.withPostValidateStates(List.of("postValidate")).withPostValidateParams(Map.of());
        return saltStateApplyRotationContextBuilder.build();
    }

}