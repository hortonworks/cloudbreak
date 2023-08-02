package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@ExtendWith(MockitoExtension.class)
public class AbstractRotationExecutorTest {

    @Mock
    private SecretRotationStepProgressService secretRotationProgressService;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private TestExecutor underTest;

    @Test
    public void testRotateWhenNoProgress() {
        when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());

        underTest.executeRotate(new RotationContext(""), TestSecretType.TEST);

        verify(secretRotationProgressService, times(1)).latestStep(any(), any(), any(), any());
        verify(secretRotationProgressService, times(0)).finished(any());
        verify(secretRotationNotificationService, times(1))
                .sendNotification("", TestSecretType.TEST, TestSecretRotationStep.STEP, RotationFlowExecutionType.ROTATE);
    }

    @Test
    public void testRotateWhenStepOngoing() {
        when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.of(new SecretRotationStepProgress("",
                TestSecretType.TEST, TestSecretRotationStep.STEP, RotationFlowExecutionType.ROTATE, System.currentTimeMillis())));
        doNothing().when(secretRotationProgressService).finished(any());

        underTest.executeRotate(new RotationContext(""), TestSecretType.TEST);

        verify(secretRotationProgressService, times(1)).latestStep(any(), any(), any(), any());
        verify(secretRotationProgressService, times(1)).finished(any());
        verify(secretRotationNotificationService, times(1))
                .sendNotification("", TestSecretType.TEST, TestSecretRotationStep.STEP, RotationFlowExecutionType.ROTATE);
    }

    @Test
    public void testRotateFailureWhenStepOngoing() {
        when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.of(new SecretRotationStepProgress("",
                TestSecretType.TEST, TestSecretRotationStep.STEP, RotationFlowExecutionType.ROTATE, System.currentTimeMillis())));
        doNothing().when(secretRotationProgressService).finished(any());

        assertThrows(SecretRotationException.class, () -> underTest.executeRotate(new RotationContext(null), TestSecretType.TEST));

        verify(secretRotationProgressService, times(1)).latestStep(any(), any(), any(), any());
        verify(secretRotationProgressService, times(1)).finished(any());
        verify(secretRotationNotificationService, times(1))
                .sendNotification(null, TestSecretType.TEST, TestSecretRotationStep.STEP, RotationFlowExecutionType.ROTATE);
    }

    @Test
    public void testRotateWhenStepAlreadyFinished() {
        SecretRotationStepProgress progress = new SecretRotationStepProgress("", TestSecretType.TEST, TestSecretRotationStep.STEP,
                RotationFlowExecutionType.ROTATE, System.currentTimeMillis());
        progress.setFinished(System.currentTimeMillis());
        when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.of(progress));

        underTest.executeRotate(new RotationContext(""), TestSecretType.TEST);

        verify(secretRotationProgressService, times(1)).latestStep(any(), any(), any(), any());
        verify(secretRotationProgressService, times(0)).finished(any());
        verify(secretRotationNotificationService, times(0)).sendNotification(any(), any(), any(), any());
    }

    @Test
    public void testPreValidation() {
        when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());

        underTest.executePreValidation(new RotationContext(""), TestSecretType.TEST);

        verify(secretRotationProgressService, times(1)).latestStep(any(), any(), any(), any());
    }

    @Test
    public void testPreValidationFailure() {
        when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());

        assertThrows(SecretRotationException.class, () -> underTest.executePreValidation(new RotationContext(null), TestSecretType.TEST));

        verify(secretRotationProgressService, times(1)).latestStep(any(), any(), any(), any());
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

    private static class TestExecutor extends AbstractRotationExecutor<RotationContext> {

        @Override
        protected void rotate(RotationContext rotationContext) throws Exception {
            simulateFailure(rotationContext);
        }

        @Override
        protected void rollback(RotationContext rotationContext) throws Exception {
            simulateFailure(rotationContext);
        }

        @Override
        protected void finalize(RotationContext rotationContext) throws Exception {
            simulateFailure(rotationContext);
        }

        @Override
        protected void preValidate(RotationContext rotationContext) throws Exception {
            simulateFailure(rotationContext);
        }

        @Override
        protected void postValidate(RotationContext rotationContext) throws Exception {
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
