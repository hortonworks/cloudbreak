package com.sequenceiq.freeipa.service.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.rotation.RotationMetadataTestUtil;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.freeipa.service.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.freeipa.service.rotation.context.SaltStateApplyRotationContext.SaltStateApplyRotationContextBuilder;

@ExtendWith(MockitoExtension.class)
public class SaltStateApplyRotationExecutorTest {

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private SecretRotationStepProgressService secretRotationProgressService;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private SaltStateApplyRotationExecutor underTest;

    @BeforeEach
    public void mockProgressService() {
        lenient().when(secretRotationProgressService.latestStep(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    public void testPreValidation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(hostOrchestrator).ping(any(), any());
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executePreValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(hostOrchestrator).ping(any(), any());
        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("preValidate")), any(), any(), any());
    }

    @Test
    public void testPreValidationIfPingFails() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("pingpong")).when(hostOrchestrator).ping(any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executePreValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null));

        verify(hostOrchestrator).ping(any(), any());
        verifyNoMoreInteractions(hostOrchestrator);
    }

    @Test
    public void testPostValidation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executePostValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("postValidate")), any(), any(), any());
    }

    @Test
    public void testRotation() throws Exception {
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("state")), any(), any(), any());
    }

    @Test
    public void testRotationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()),
                        RotationMetadataTestUtil.metadataForRotation("resource", null)));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("state")), any(), any(), any());
    }

    @Test
    public void testRollbackFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()),
                        RotationMetadataTestUtil.metadataForRollback("resource", null)));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("rollback")), any(), any(), any());
    }

    @Test
    public void testRollback() throws Exception {
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()), null);

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("rollback")), any(), any(), any());
    }

    @Test
    public void testRollbackWithoutRollbackStates() throws Exception {
        underTest.executeRollback(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verifyNoInteractions(hostOrchestrator);
    }

    @Test
    public void testFinalization() throws Exception {
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))), null);

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("finalize")), any(), any(), any());
    }

    @Test
    public void testFinalizationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))),
                        RotationMetadataTestUtil.metadataForFinalize("resource", null)));

        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("finalize")), any(), any(), any());
    }

    @Test
    public void testFinalizationIfNotNeeded() throws Exception {
        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verifyNoInteractions(hostOrchestrator);
    }

    private SaltStateApplyRotationContext createContext(List<String> states, Optional<List<String>> rollbackStates, Optional<List<String>> finalizeStates) {
        SaltStateApplyRotationContextBuilder saltStateApplyRotationContextBuilder = SaltStateApplyRotationContext.builder().withStates(states);
        rollbackStates.ifPresent(saltStateApplyRotationContextBuilder::withRollbackStates);
        finalizeStates.ifPresent(saltStateApplyRotationContextBuilder::withCleanupStates);
        saltStateApplyRotationContextBuilder.withPreValidateStates(List.of("preValidate"));
        saltStateApplyRotationContextBuilder.withPostValidateStates(List.of("postValidate"));
        return saltStateApplyRotationContextBuilder.build();
    }
}
