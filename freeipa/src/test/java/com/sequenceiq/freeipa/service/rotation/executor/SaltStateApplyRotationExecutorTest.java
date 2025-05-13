package com.sequenceiq.freeipa.service.rotation.executor;

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
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.freeipa.service.rotation.SecretRotationSaltService;
import com.sequenceiq.freeipa.service.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.freeipa.service.rotation.context.SaltStateApplyRotationContext.SaltStateApplyRotationContextBuilder;

@ExtendWith(MockitoExtension.class)
public class SaltStateApplyRotationExecutorTest {

    @Mock
    private SecretRotationSaltService secretRotationSaltService;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private SaltStateApplyRotationExecutor underTest;

    @Test
    public void testPreValidation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(secretRotationSaltService).validateSalt(any(), any());
        doNothing().when(secretRotationSaltService).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executePreValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(secretRotationSaltService).validateSalt(any(), any());
        verify(secretRotationSaltService).executeSaltState(any(), any(), eq(List.of("preValidate")), any(), any(), any());
    }

    @Test
    public void testPreValidationIfvalidateSaltFails() {
        doThrow(new SecretRotationException("validateSaltpong")).when(secretRotationSaltService).validateSalt(any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executePreValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null));

        verify(secretRotationSaltService).validateSalt(any(), any());
        verifyNoMoreInteractions(secretRotationSaltService);
    }

    @Test
    public void testPostValidation() throws CloudbreakOrchestratorFailedException {
        doNothing().when(secretRotationSaltService).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executePostValidation(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(secretRotationSaltService).executeSaltState(any(), any(), eq(List.of("postValidate")), any(), any(), any());
    }

    @Test
    public void testRotation() throws Exception {
        doNothing().when(secretRotationSaltService).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verify(secretRotationSaltService).executeSaltState(any(), any(), eq(List.of("state")), any(), any(), any());
    }

    @Test
    public void testRotationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something"))
                .when(secretRotationSaltService).executeSaltState(any(), any(), any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(createContext(List.of("state"), Optional.empty(), Optional.empty()),
                        RotationMetadataTestUtil.metadataForRotation("resource", null)));

        verify(secretRotationSaltService).executeSaltState(any(), any(), eq(List.of("state")), any(), any(), any());
    }

    @Test
    public void testRollbackFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something"))
                .when(secretRotationSaltService).executeSaltState(any(), any(), any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()),
                        RotationMetadataTestUtil.metadataForRollback("resource", null)));

        verify(secretRotationSaltService).executeSaltState(any(), any(), eq(List.of("rollback")), any(), any(), any());
    }

    @Test
    public void testRollback() throws Exception {
        doNothing().when(secretRotationSaltService).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeRollback(createContext(List.of("state"), Optional.of(List.of("rollback")), Optional.empty()), null);

        verify(secretRotationSaltService).executeSaltState(any(), any(), eq(List.of("rollback")), any(), any(), any());
    }

    @Test
    public void testRollbackWithoutRollbackStates() throws Exception {
        underTest.executeRollback(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verifyNoInteractions(secretRotationSaltService);
    }

    @Test
    public void testFinalization() throws Exception {
        doNothing().when(secretRotationSaltService).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))), null);

        verify(secretRotationSaltService).executeSaltState(any(), any(), eq(List.of("finalize")), any(), any(), any());
    }

    @Test
    public void testFinalizationFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("something"))
                .when(secretRotationSaltService).executeSaltState(any(), any(), any(), any(), any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.of(List.of("finalize"))),
                        RotationMetadataTestUtil.metadataForFinalize("resource", null)));

        verify(secretRotationSaltService).executeSaltState(any(), any(), eq(List.of("finalize")), any(), any(), any());
    }

    @Test
    public void testFinalizationIfNotNeeded() throws Exception {
        underTest.executeFinalize(createContext(List.of("state"), Optional.empty(), Optional.empty()), null);

        verifyNoInteractions(secretRotationSaltService);
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
