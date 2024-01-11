package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.rotation.RotationMetadataTestUtil;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext.SaltStateApplyRotationContextBuilder;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;

@ExtendWith(MockitoExtension.class)
public class SaltStateApplyRotationExecutorTest {

    @Mock
    private SecretRotationSaltService saltService;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private SaltStateApplyRotationExecutor underTest;

    @Test
    public void testPreValidation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(saltService).validateSalt(any(), any());
        doNothing().when(saltService).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executePreValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(saltService).validateSalt(any(), any());
        verify(saltService).executeSaltState(any(), any(), eq(List.of("preValidate")), any(), any(), any());
    }

    @Test
    public void testPreValidationIfvalidateSaltFails() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("validateSaltpong")).when(saltService).validateSalt(any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executePreValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null));

        verify(saltService).validateSalt(any(), any());
        verifyNoMoreInteractions(saltService);
    }

    @Test
    public void testPostValidation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(saltService).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executePostValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(saltService).executeSaltState(any(), any(), eq(List.of("postValidate")), any(), any(), any());
    }

    @Test
    public void testRotation() throws Exception {
        doNothing().when(saltService).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(saltService).executeSaltState(any(), any(), eq(List.of("state")), any(), any(), any());
    }

    @Test
    public void testRotationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(saltService).executeSaltState(any(), any(), any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()),
                        RotationMetadataTestUtil.metadataForRotation("resource", null)));

        verify(saltService).executeSaltState(any(), any(), eq(List.of("state")), any(), any(), any());
    }

    @Test
    public void testRollbackFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(saltService).executeSaltState(any(), any(), any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()),
                        RotationMetadataTestUtil.metadataForRollback("resource", null)));

        verify(saltService).executeSaltState(any(), any(), eq(List.of("rollback")), any(), any(), any());
    }

    @Test
    public void testRollback() throws Exception {
        doNothing().when(saltService).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()), null);

        verify(saltService).executeSaltState(any(), any(), eq(List.of("rollback")), any(), any(), any());
    }

    @Test
    public void testRollbackWithoutRollbackStates() throws Exception {
        underTest.executeRollback(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verifyNoInteractions(saltService);
    }

    @Test
    public void testFinalization() throws Exception {
        doNothing().when(saltService).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))), null);

        verify(saltService).executeSaltState(any(), any(), eq(List.of("finalize")), any(), any(), any());
    }

    @Test
    public void testFinalizationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something")).when(saltService).executeSaltState(any(), any(), any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))),
                        RotationMetadataTestUtil.metadataForFinalize("resource", null)));

        verify(saltService).executeSaltState(any(), any(), eq(List.of("finalize")), any(), any(), any());
    }

    @Test
    public void testFinalizationIfNotNeeded() throws Exception {
        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verifyNoInteractions(saltService);
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
