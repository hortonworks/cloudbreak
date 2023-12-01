package com.sequenceiq.datalake.service.rotation.notification;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS;
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
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;

@ExtendWith(MockitoExtension.class)
class SdxSecretRotationNotificationServiceTest {

    private static final RotationMetadata METADATA = new RotationMetadata(DATALAKE_SALT_BOOT_SECRETS, ROTATE, null,
            "", Optional.empty(), null);

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

        underTest.sendNotification(METADATA, CLOUDBREAK_ROTATE_POLLING);

        verify(sdxNotificationService).send(ResourceEvent.SECRET_ROTATION_STEP,
                List.of("Execute secret [random secret]: small step for a secret, big step for the customer"), cluster);
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS");
    }

    @Test
    public void testRollbackNotification() {
        SdxCluster cluster = new SdxCluster();
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(anyString())).thenReturn(Optional.of(cluster));

        underTest.sendNotification(METADATA, CLOUDBREAK_ROTATE_POLLING);

        verify(sdxNotificationService).send(ResourceEvent.SECRET_ROTATION_STEP,
                List.of("Execute secret [random secret]: small step for a secret, big step for the customer"), cluster);
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS");
    }

    @Test
    public void testFinalizeNotification() {
        SdxCluster cluster = new SdxCluster();
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(anyString())).thenReturn(Optional.of(cluster));

        underTest.sendNotification(METADATA, CLOUDBREAK_ROTATE_POLLING);

        verify(sdxNotificationService).send(ResourceEvent.SECRET_ROTATION_STEP,
                List.of("Execute secret [random secret]: small step for a secret, big step for the customer"), cluster);
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS");
    }
}