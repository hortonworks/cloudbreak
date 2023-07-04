package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationProgressService;

@ExtendWith(MockitoExtension.class)
public class AbstractRotationExecutorTest {

    @Mock
    private SecretRotationProgressService secretRotationProgressService;

    @InjectMocks
    private TestExecutor underTest;

    @BeforeEach
    public void mockProgressService() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.of(secretRotationProgressService), true);
    }

    @Test
    public void testRotateWhenNoProgress() {
        when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());

        underTest.executeRotate(new RotationContext(""), TestSecretType.TEST);

        verify(secretRotationProgressService, times(1)).latestStep(any(), any(), any(), any());
        verify(secretRotationProgressService, times(0)).isFinished(any());
        verify(secretRotationProgressService, times(0)).finished(any());
    }

    @Test
    public void testRotateWhenStepOngoing() {
        when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.of("anything"));
        when(secretRotationProgressService.isFinished(any())).thenReturn(Boolean.FALSE);
        doNothing().when(secretRotationProgressService).finished(any());

        underTest.executeRotate(new RotationContext(""), TestSecretType.TEST);

        verify(secretRotationProgressService, times(1)).latestStep(any(), any(), any(), any());
        verify(secretRotationProgressService, times(1)).isFinished(any());
        verify(secretRotationProgressService, times(1)).finished(any());
    }

    @Test
    public void testRotateFailureWhenStepOngoing() {
        when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.of("anything"));
        when(secretRotationProgressService.isFinished(any())).thenReturn(Boolean.FALSE);
        doNothing().when(secretRotationProgressService).finished(any());

        assertThrows(SecretRotationException.class, () -> underTest.executeRotate(new RotationContext(null), TestSecretType.TEST));

        verify(secretRotationProgressService, times(1)).latestStep(any(), any(), any(), any());
        verify(secretRotationProgressService, times(1)).isFinished(any());
        verify(secretRotationProgressService, times(1)).finished(any());
    }

    @Test
    public void testRotateWhenStepAlreadyFinished() {
        when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.of("anything"));
        when(secretRotationProgressService.isFinished(any())).thenReturn(Boolean.TRUE);

        underTest.executeRotate(new RotationContext(""), TestSecretType.TEST);

        verify(secretRotationProgressService, times(1)).latestStep(any(), any(), any(), any());
        verify(secretRotationProgressService, times(1)).isFinished(any());
        verify(secretRotationProgressService, times(0)).finished(any());
    }

    @Test
    public void testPreValidation() {
        underTest.executePreValidation(new RotationContext(""));

        verifyNoInteractions(secretRotationProgressService);
    }

    @Test
    public void testPreValidationFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executePreValidation(new RotationContext(null)));

        verifyNoInteractions(secretRotationProgressService);
    }

    @Test
    public void testPostValidation() {
        underTest.executePostValidation(new RotationContext(""));

        verifyNoInteractions(secretRotationProgressService);
    }

    @Test
    public void testPostValidationFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(new RotationContext(null)));

        verifyNoInteractions(secretRotationProgressService);
    }

    @Test
    public void testRollback() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.empty(), true);

        underTest.executeRollback(new RotationContext(""), TestSecretType.TEST);

        verifyNoInteractions(secretRotationProgressService);
    }

    @Test
    public void testRollbackFailure() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.empty(), true);

        assertThrows(SecretRotationException.class, () -> underTest.executeRollback(new RotationContext(null), TestSecretType.TEST));

        verifyNoInteractions(secretRotationProgressService);
    }

    @Test
    public void testFinalize() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.empty(), true);

        underTest.executeFinalize(new RotationContext(""), TestSecretType.TEST);

        verifyNoInteractions(secretRotationProgressService);
    }

    @Test
    public void testFinalizeFailure() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.empty(), true);

        assertThrows(SecretRotationException.class, () -> underTest.executeFinalize(new RotationContext(null), TestSecretType.TEST));

        verifyNoInteractions(secretRotationProgressService);
    }

    private static class TestExecutor extends AbstractRotationExecutor<RotationContext> {

        @Override
        public void rotate(RotationContext rotationContext) throws Exception {
            simulateFailure(rotationContext);
        }

        @Override
        public void rollback(RotationContext rotationContext) throws Exception {
            simulateFailure(rotationContext);
        }

        @Override
        public void finalize(RotationContext rotationContext) throws Exception {
            simulateFailure(rotationContext);
        }

        @Override
        public void preValidate(RotationContext rotationContext) throws Exception {
            simulateFailure(rotationContext);
        }

        @Override
        public void postValidate(RotationContext rotationContext) throws Exception {
            simulateFailure(rotationContext);
        }

        private void simulateFailure(RotationContext rotationContext) {
            if (rotationContext.getResourceCrn() == null) {
                throw new CloudbreakServiceException("oops");
            }
        }

        @Override
        public SecretRotationStep getType() {
            return TestSecretRotationStep.STEP;
        }

        @Override
        public Class<RotationContext> getContextClass() {
            return RotationContext.class;
        }
    }

}
