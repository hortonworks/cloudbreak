package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.redbeams.rotation.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.RotationMetadataTestUtil;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;

@ExtendWith(MockitoExtension.class)
class RedbeamsPollerRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private SdxRotationService sdxRotationService;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private RedbeamsPollerRotationExecutor underTest;

    @Test
    void rotateShouldThrowSecretRotationExceptionIfCloudbreakRotationFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateRedbeamsSecret(anyString(),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROTATE));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForRotation(RESOURCE_CRN, null)));
        assertEquals("Execution of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null, reason: error",
                secretRotationException.getMessage());
    }

    @Test
    void rotateShouldSucceed() {
        underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null);
        verify(sdxRotationService, times(1)).rotateRedbeamsSecret(eq(RESOURCE_CRN),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROTATE));
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfCloudbreakRollbackFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateRedbeamsSecret(anyString(),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROLLBACK));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForRollback(RESOURCE_CRN, null)));
        assertEquals("Rollback of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null, reason: error",
                secretRotationException.getMessage());
    }

    @Test
    void rollbackShouldSucceed() {
        underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null);
        verify(sdxRotationService, times(1)).rotateRedbeamsSecret(eq(RESOURCE_CRN),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROLLBACK));
    }

    @Test
    void finalizeShouldThrowSecretRotationExceptionIfCloudbreakFinalizeFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateRedbeamsSecret(anyString(),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(FINALIZE));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForFinalize(RESOURCE_CRN, null)));
        assertEquals("Finalization of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null, reason: error",
                secretRotationException.getMessage());
    }

    @Test
    void finalizeShouldSucceed() {
        underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null);
        verify(sdxRotationService, times(1)).rotateRedbeamsSecret(eq(RESOURCE_CRN),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(FINALIZE));
    }

    @Test
    void preValidateShouldSucceed() {
        underTest.preValidate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD));
        verify(sdxRotationService, times(1)).preValidateRedbeamsRotation(eq(RESOURCE_CRN));
    }
}