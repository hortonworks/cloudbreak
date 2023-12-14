package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.FREEIPA_KERBEROS_BIND_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
class FreeipaPollerRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private SdxRotationService sdxRotationService;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private FreeipaPollerRotationExecutor underTest;

    @Test
    void rotateShouldThrowSecretRotationExceptionIfCloudbreakRotationFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateFreeipaSecret(anyString(),
                eq(FREEIPA_KERBEROS_BIND_USER), eq(ROTATE), any());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, FREEIPA_KERBEROS_BIND_USER),
                        RotationMetadataTestUtil.metadataForRotation(RESOURCE_CRN, null)));
        assertEquals("Execution of rotation failed at FREEIPA_ROTATE_POLLING step for resourceCrn regarding secret null, reason: error",
                secretRotationException.getMessage());
    }

    @Test
    void rotateShouldSucceed() {
        underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, FREEIPA_KERBEROS_BIND_USER),
                RotationMetadataTestUtil.metadataForRotation(RESOURCE_CRN, null));
        verify(sdxRotationService, times(1)).rotateFreeipaSecret(eq(RESOURCE_CRN),
                eq(FREEIPA_KERBEROS_BIND_USER), eq(ROTATE), any());
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfCloudbreakRollbackFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateFreeipaSecret(anyString(),
                eq(FREEIPA_KERBEROS_BIND_USER), eq(ROLLBACK), any());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, FREEIPA_KERBEROS_BIND_USER),
                        RotationMetadataTestUtil.metadataForRollback(RESOURCE_CRN, null)));
        assertEquals("Rollback of rotation failed at FREEIPA_ROTATE_POLLING step for resourceCrn regarding secret null, reason: error",
                secretRotationException.getMessage());
    }

    @Test
    void rollbackShouldSucceed() {
        underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, FREEIPA_KERBEROS_BIND_USER),
                RotationMetadataTestUtil.metadataForRollback(RESOURCE_CRN, null));
        verify(sdxRotationService, times(1)).rotateFreeipaSecret(eq(RESOURCE_CRN),
                eq(FREEIPA_KERBEROS_BIND_USER), eq(ROLLBACK), any());
    }

    @Test
    void finalizeShouldThrowSecretRotationExceptionIfCloudbreakFinalizeFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateFreeipaSecret(anyString(),
                eq(FREEIPA_KERBEROS_BIND_USER), eq(FINALIZE), any());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, FREEIPA_KERBEROS_BIND_USER),
                        RotationMetadataTestUtil.metadataForFinalize(RESOURCE_CRN, null)));
        assertEquals("Finalization of rotation failed at FREEIPA_ROTATE_POLLING step for resourceCrn regarding secret null, reason: error",
                secretRotationException.getMessage());
    }

    @Test
    void finalizeShouldSucceed() {
        underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, FREEIPA_KERBEROS_BIND_USER),
                RotationMetadataTestUtil.metadataForFinalize(RESOURCE_CRN, null));
        verify(sdxRotationService, times(1)).rotateFreeipaSecret(eq(RESOURCE_CRN),
                eq(FREEIPA_KERBEROS_BIND_USER), eq(FINALIZE), any());
    }

    @Test
    void preValidateShouldSucceed() {
        underTest.preValidate(new PollerRotationContext(RESOURCE_CRN, FREEIPA_KERBEROS_BIND_USER));
        verify(sdxRotationService, times(1)).preValidateFreeipaRotation(eq(RESOURCE_CRN));
    }

}