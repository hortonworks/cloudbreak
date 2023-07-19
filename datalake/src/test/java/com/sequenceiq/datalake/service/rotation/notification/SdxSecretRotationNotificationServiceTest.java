package com.sequenceiq.datalake.service.rotation.notification;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@ExtendWith(MockitoExtension.class)
class SdxSecretRotationNotificationServiceTest {

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxNotificationService sdxNotificationService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @InjectMocks
    private SdxSecretRotationNotificationService underTest;

    @BeforeEach
    public void setUp() {
        when(cloudbreakMessagesService.getMessage(anyString()))
                .thenReturn("Execute")
                .thenReturn("random secret")
                .thenReturn("small step for a secret, big step for the customer");
    }

    @Test
    public void testRotationNotification() {
        SdxCluster cluster = new SdxCluster();
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(anyString())).thenReturn(Optional.of(cluster));

        underTest.sendNotification("crn", DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS, CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING,
                RotationFlowExecutionType.ROTATE);

        verify(sdxNotificationService).send(ResourceEvent.SECRET_ROTATION_STEP,
                List.of("Execute secret [random secret]: small step for a secret, big step for the customer"), cluster);
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS");
    }

    @Test
    public void testRollbackNotification() {
        SdxCluster cluster = new SdxCluster();
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(anyString())).thenReturn(Optional.of(cluster));

        underTest.sendNotification("crn", DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS, CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING,
                RotationFlowExecutionType.ROLLBACK);

        verify(sdxNotificationService).send(ResourceEvent.SECRET_ROTATION_STEP,
                List.of("Execute secret [random secret]: small step for a secret, big step for the customer"), cluster);
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS");
    }

    @Test
    public void testFinalizeNotification() {
        SdxCluster cluster = new SdxCluster();
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(anyString())).thenReturn(Optional.of(cluster));

        underTest.sendNotification("crn", DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS, CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING,
                RotationFlowExecutionType.FINALIZE);

        verify(sdxNotificationService).send(ResourceEvent.SECRET_ROTATION_STEP,
                List.of("Execute secret [random secret]: small step for a secret, big step for the customer"), cluster);
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS");
    }
}