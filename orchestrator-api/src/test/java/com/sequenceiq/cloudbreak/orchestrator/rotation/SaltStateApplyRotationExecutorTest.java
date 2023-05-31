package com.sequenceiq.cloudbreak.orchestrator.rotation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;

@ExtendWith(MockitoExtension.class)
public class SaltStateApplyRotationExecutorTest {

    @Mock
    private HostOrchestrator hostOrchestrator;

    @InjectMocks
    private SaltStateApplyRotationExecutor underTest;

    @Test
    public void testRotation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any());

        underTest.rotate(createContext(List.of("state"), Optional.empty(), Optional.empty()));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("state")), any());
    }

    @Test
    public void testRotationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).executeSaltState(any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.rotate(createContext(List.of("state"), Optional.empty(), Optional.empty())));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("state")), any());
    }

    @Test
    public void testRollbackFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).executeSaltState(any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.rollback(createContext(List.of("state"), Optional.empty(), Optional.empty())));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("state")), any());
    }

    @Test
    public void testRollbackWithOwnStates() throws CloudbreakOrchestratorFailedException {
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any());

        underTest.rollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("rollback")), any());
    }

    @Test
    public void testRollbackWithOriginalStates() throws CloudbreakOrchestratorFailedException {
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any());

        underTest.rollback(createContext(List.of("state"), Optional.empty(), Optional.empty()));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("state")), any());
    }

    @Test
    public void testFinalization() throws CloudbreakOrchestratorFailedException {
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any());

        underTest.finalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("finalize")), any());
    }

    @Test
    public void testFinalizationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).executeSaltState(any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.finalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize")))));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("finalize")), any());
    }

    @Test
    public void testFinalizationIfNotNeeded() {
        underTest.finalize(createContext(List.of("state"), Optional.empty(), Optional.empty()));

        verifyNoInteractions(hostOrchestrator);
    }

    private SaltStateApplyRotationContext createContext(List<String> states, Optional<List<String>> rollbackStates, Optional<List<String>> finalizeStates) {
        return SaltStateApplyRotationContext.builder()
                .withStates(states)
                .withRollbackStates(rollbackStates)
                .withCleanupStates(finalizeStates)
                .build();
    }
}
