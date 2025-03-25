package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;

@ExtendWith(MockitoExtension.class)
class AbstractRotationExecutorTest {

    private static final RotationMetadata METADATA = new RotationMetadata(TEST, ROTATE, null, "", null);

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @Mock
    private Logger logger;

    private TestExecutor underTest;

    @BeforeEach
    void init() {
        underTest = new TestExecutor(logger);
    }

    @Test
    void testRotate() {
        underTest.executeRotate(new RotationContext(""), METADATA);

        verify(logger).info(eq("rotate"));
    }

    @Test
    void testRotateFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executeRotate(new RotationContext(null), METADATA));

        verify(logger).info(eq("rotate"));
    }

    @Test
    void testRollback() {
        underTest.executeRollback(new RotationContext(""), METADATA);

        verify(logger).info(eq("rollback"));
    }

    @Test
    void testRollbackFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executeRollback(new RotationContext(null), METADATA));

        verify(logger).info(eq("rollback"));
    }

    @Test
    void testFinalizeRotation() {
        underTest.executeFinalize(new RotationContext(""), METADATA);

        verify(logger).info(eq("finalize"));
    }

    @Test
    void testFinalizeRotationFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executeFinalize(new RotationContext(null), METADATA));

        verify(logger).info(eq("finalize"));
    }

    @Test
    void testPreValidate() {
        underTest.executePreValidation(new RotationContext(""), METADATA);

        verify(logger).info(eq("preValidate"));
    }

    @Test
    void testPreValidateFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executePreValidation(new RotationContext(null), METADATA));

        verify(logger).info(eq("preValidate"));
    }

    @Test
    void testPostValidate() {
        underTest.executePostValidation(new RotationContext(""), METADATA);

        verify(logger).info(eq("postValidate"));
    }

    @Test
    void testPostValidateFailure() {
        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(new RotationContext(null), METADATA));

        verify(logger).info(eq("postValidate"));
    }

    private static class TestExecutor extends AbstractRotationExecutor<RotationContext> {

        private Logger logger;

        TestExecutor(Logger logger) {
            this.logger = logger;
        }

        @Override
        protected void rotate(RotationContext rotationContext) {
            logger.info("rotate");
            simulateFailure(rotationContext);
        }

        @Override
        protected void rollback(RotationContext rotationContext) {
            logger.info("rollback");
            simulateFailure(rotationContext);
        }

        @Override
        protected void finalizeRotation(RotationContext rotationContext) {
            logger.info("finalize");
            simulateFailure(rotationContext);
        }

        @Override
        protected void preValidate(RotationContext rotationContext) {
            logger.info("preValidate");
            simulateFailure(rotationContext);
        }

        @Override
        protected void postValidate(RotationContext rotationContext) {
            logger.info("postValidate");
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
