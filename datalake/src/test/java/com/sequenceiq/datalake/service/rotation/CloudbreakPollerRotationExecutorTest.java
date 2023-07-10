package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationProgressService;

@ExtendWith(MockitoExtension.class)
class CloudbreakPollerRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private SdxRotationService sdxRotationService;

    @Mock
    private SecretRotationProgressService secretRotationProgressService;

    @InjectMocks
    private CloudbreakPollerRotationExecutor underTest;

    @BeforeEach
    public void mockProgressService() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.of(secretRotationProgressService), true);
        lenient().when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfCloudbreakRotationFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateCloudbreakSecret(anyString(),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROTATE));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Execution of rotation failed at CLOUDBREAK_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void rotateShouldSucceed() {
        underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), null);
        verify(sdxRotationService, times(1)).rotateCloudbreakSecret(eq(RESOURCE_CRN),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROTATE));
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfCloudbreakRollbackFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateCloudbreakSecret(anyString(),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROLLBACK));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Rollback of rotation failed at CLOUDBREAK_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void rollbackShouldSucceed() {
        underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), null);
        verify(sdxRotationService, times(1)).rotateCloudbreakSecret(eq(RESOURCE_CRN),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROLLBACK));
    }

    @Test
    void finalizeShouldThrowSecretRotationExceptionIfCloudbreakFinalizeFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateCloudbreakSecret(anyString(),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(FINALIZE));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Finalization of rotation failed at CLOUDBREAK_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void finalizeShouldSucceed() {
        underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), null);
        verify(sdxRotationService, times(1)).rotateCloudbreakSecret(eq(RESOURCE_CRN),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(FINALIZE));
    }

}