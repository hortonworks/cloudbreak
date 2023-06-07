package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

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
import com.sequenceiq.cloudbreak.rotation.context.SaltRunOrchestratorStateRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltRunOrchestratorStateRotationContext.SaltRunOrchestratorStateRotationContextBuilder;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;

@ExtendWith(MockitoExtension.class)
class SaltRunOrchestratorStateRotationExecutorTest {
    @Mock
    private HostOrchestrator hostOrchestrator;

    @InjectMocks
    private SaltRunOrchestratorStateRotationExecutor underTest;

    @Captor
    private ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsArgumentCaptor;

    @Test
    public void testRotation() throws Exception {
        doNothing().when(hostOrchestrator).runOrchestratorState(any());

        underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()));

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("state", orchestratorStateParams.getState());
    }

    @Test
    public void testRotationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).runOrchestratorState(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty())));

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("state", orchestratorStateParams.getState());
    }

    @Test
    public void testRollbackFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).runOrchestratorState(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty())));

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("rollback", orchestratorStateParams.getState());
    }

    @Test
    public void testRollbackWithOwnStates() throws Exception {
        doNothing().when(hostOrchestrator).runOrchestratorState(any());

        underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()));

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("rollback", orchestratorStateParams.getState());
    }

    @Test
    public void testFinalization() throws Exception {
        doNothing().when(hostOrchestrator).runOrchestratorState(any());

        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))));

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("finalize", orchestratorStateParams.getState());
    }

    @Test
    public void testFinalizationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).runOrchestratorState(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize")))));

        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsArgumentCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsArgumentCaptor.getValue();
        assertEquals("finalize", orchestratorStateParams.getState());
    }

    @Test
    public void testFinalizationIfNotNeeded() throws Exception {
        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.empty()));

        verifyNoInteractions(hostOrchestrator);
    }

    private SaltRunOrchestratorStateRotationContext createContext(List<String> states, Optional<List<String>> rollbackStates,
            Optional<List<String>> finalizeStates) {
        SaltRunOrchestratorStateRotationContextBuilder saltStateApplyRotationContextBuilder = new SaltRunOrchestratorStateRotationContextBuilder()
                .withStates(states);
        if (rollbackStates.isPresent()) {
            saltStateApplyRotationContextBuilder.withRollbackStates(rollbackStates)
                    .withRollbackParams(Optional.of(new HashMap<>()));
        }
        if (finalizeStates.isPresent()) {
            saltStateApplyRotationContextBuilder.withCleanupStates(finalizeStates)
                    .withCleanupParams(Optional.of(new HashMap<>()));
        }
        return saltStateApplyRotationContextBuilder.build();
    }

}