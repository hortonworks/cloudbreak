package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.secret.type.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.context.RedbeamsPollerRotationContext;

@ExtendWith(MockitoExtension.class)
class RedbeamsPollerRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private SdxRotationService sdxRotationService;

    @InjectMocks
    private RedbeamsPollerRotationExecutor underTest;

    @Test
    void rotateShouldThrowSecretRotationExceptionIfCloudbreakRotationFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateRedbeamsSecret(anyString(),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD.name()), eq(ROTATE));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.rotate(new RedbeamsPollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD)));
        Assertions.assertEquals("Rotate redbeams secret failed", secretRotationException.getMessage());
    }

    @Test
    void rotateShouldSucceed() {
        underTest.rotate(new RedbeamsPollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD));
        verify(sdxRotationService, times(1)).rotateRedbeamsSecret(eq(RESOURCE_CRN),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD.name()), eq(ROTATE));
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfCloudbreakRollbackFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateRedbeamsSecret(anyString(),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD.name()), eq(ROLLBACK));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.rollback(new RedbeamsPollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD)));
        Assertions.assertEquals("Rollback redbeams secret failed", secretRotationException.getMessage());
    }

    @Test
    void rollbackShouldSucceed() {
        underTest.rollback(new RedbeamsPollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD));
        verify(sdxRotationService, times(1)).rotateRedbeamsSecret(eq(RESOURCE_CRN),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD.name()), eq(ROLLBACK));
    }

    @Test
    void finalizeShouldThrowSecretRotationExceptionIfCloudbreakFinalizeFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateRedbeamsSecret(anyString(),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD.name()), eq(FINALIZE));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.finalize(new RedbeamsPollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD)));
        Assertions.assertEquals("Finalize redbeams secret failed", secretRotationException.getMessage());
    }

    @Test
    void finalizeShouldSucceed() {
        underTest.finalize(new RedbeamsPollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD));
        verify(sdxRotationService, times(1)).rotateRedbeamsSecret(eq(RESOURCE_CRN),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD.name()), eq(FINALIZE));
    }
}