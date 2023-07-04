package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.SaltRunOrchestratorStateRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltRunOrchestratorStateRotationContext.SaltRunOrchestratorStateRotationContextBuilder;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@ExtendWith(MockitoExtension.class)
class SaltRunOrchestratorStateRotationExecutorTest {
    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private SecretRotationStepProgressService secretRotationProgressService;

    @InjectMocks
    private SaltRunOrchestratorStateRotationExecutor underTest;

    @Captor
    private ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsArgumentCaptor;

    @BeforeEach
    public void mockProgressService() {
        lenient().when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    public void testPreValidation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(hostOrchestrator).ping(any(), any());
        doNothing().when(hostOrchestrator).runOrchestratorState(any());

        underTest.executePreValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()));

        verify(hostOrchestrator).ping(any(), any());
        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("preValidate", orchestratorStateParams.getState());
    }

    @Test
    public void testPreValidationIfPingFails() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("pingpong")).when(hostOrchestrator).ping(any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executePreValidation(createContext(List.of("state"), Optional.empty(), Optional.empty())));

        verify(hostOrchestrator).ping(any(), any());
        verifyNoMoreInteractions(hostOrchestrator);
    }

    @Test
    public void testPostValidation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(hostOrchestrator).runOrchestratorState(any());

        underTest.executePostValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()));

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("postValidate", orchestratorStateParams.getState());
    }

    @Test
    public void testRotation() throws Exception {
        doNothing().when(hostOrchestrator).runOrchestratorState(any());

        underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("state", orchestratorStateParams.getState());
    }

    @Test
    public void testRotationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).runOrchestratorState(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()), null));

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("state", orchestratorStateParams.getState());
    }

    @Test
    public void testRollbackFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).runOrchestratorState(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()), null));

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("rollback", orchestratorStateParams.getState());
    }

    @Test
    public void testRollbackWithOwnStates() throws Exception {
        doNothing().when(hostOrchestrator).runOrchestratorState(any());

        underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()), null);

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("rollback", orchestratorStateParams.getState());
    }

    @Test
    public void testFinalization() throws Exception {
        doNothing().when(hostOrchestrator).runOrchestratorState(any());

        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))), null);

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("finalize", orchestratorStateParams.getState());
    }

    @Test
    public void testFinalizationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).runOrchestratorState(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))), null));

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("finalize", orchestratorStateParams.getState());
    }

    @Test
    public void testFinalizationIfNotNeeded() throws Exception {
        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verifyNoInteractions(hostOrchestrator);
    }

    private SaltRunOrchestratorStateRotationContext createContext(List<String> states, Optional<List<String>> rollbackStates,
            Optional<List<String>> finalizeStates) {
        SaltRunOrchestratorStateRotationContextBuilder saltStateApplyRotationContextBuilder = new SaltRunOrchestratorStateRotationContextBuilder()
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