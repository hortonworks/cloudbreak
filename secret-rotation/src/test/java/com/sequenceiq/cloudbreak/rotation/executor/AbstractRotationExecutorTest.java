package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

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
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;

@ExtendWith(MockitoExtension.class)
public class AbstractRotationExecutorTest {

    private static final RotationMetadata METADATA = new RotationMetadata(TEST, ROTATE, null, "", Optional.empty());

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private TestExecutor underTest;

    @Test
    public void testRotate() {
        underTest.executeRotate(new RotationContext(""), METADATA);

        verify(secretRotationNotificationService).sendNotification(any(), any());
    }

    @Test
    public void testRotateFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executeRotate(new RotationContext(null), METADATA));

        verify(secretRotationNotificationService).sendNotification(any(), any());
    }

    @Test
    public void testRollback() {
        underTest.executeRollback(new RotationContext(""), METADATA);

        verify(secretRotationNotificationService).sendNotification(any(), any());
    }

    @Test
    public void testRollbackFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executeRollback(new RotationContext(null), METADATA));

        verify(secretRotationNotificationService).sendNotification(any(), any());
    }

    @Test
    public void testFinalize() {
        underTest.executeFinalize(new RotationContext(""), METADATA);

        verify(secretRotationNotificationService).sendNotification(any(), any());
    }

    @Test
    public void testFinalizeFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executeFinalize(new RotationContext(null), METADATA));

        verify(secretRotationNotificationService).sendNotification(any(), any());
    }

    @Test
    public void testPreValidate() {
        underTest.executePreValidation(new RotationContext(""), METADATA);

        verify(secretRotationNotificationService).sendNotification(any(), any());
    }

    @Test
    public void testPreValidateFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executePreValidation(new RotationContext(null), METADATA));

        verify(secretRotationNotificationService).sendNotification(any(), any());
    }

    @Test
    public void testPostValidate() {
        underTest.executePostValidation(new RotationContext(""), METADATA);

        verify(secretRotationNotificationService, times(0)).sendNotification(any(), any());
    }

    @Test
    public void testPostValidateFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(new RotationContext(null), METADATA));

        verify(secretRotationNotificationService, times(0)).sendNotification(any(), any());
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
