package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.SALT_BOOT_SECRETS;
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
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class StackSecretRotationNotificationServiceTest {

    private static final Long STACK_ID = 1L;

    private static final RotationMetadata METADATA = new RotationMetadata(SALT_BOOT_SECRETS, ROTATE, null,
            "", Optional.empty(), null);

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @InjectMocks
    private StackSecretRotationNotificationService underTest;

    @Mock
    private StackView stackView;

    @BeforeEach
    public void setUp() {
        when(stackView.getId()).thenReturn(STACK_ID);
        when(cloudbreakMessagesService.getMessage(anyString()))
                .thenReturn("Execute")
                .thenReturn("random secret")
                .thenReturn("small step for a secret, big step for the customer");
    }

    @Test
    public void testRotationNotification() {
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stackView);

        underTest.sendNotification(METADATA, CLOUDBREAK_ROTATE_POLLING);

        verifyEventIsSent("Execute secret [random secret]: small step for a secret, big step for the customer");
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.SALT_BOOT_SECRETS");
    }

    @Test
    public void testRollbackNotification() {
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stackView);

        underTest.sendNotification(METADATA, CLOUDBREAK_ROTATE_POLLING);

        verifyEventIsSent("Execute secret [random secret]: small step for a secret, big step for the customer");
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.SALT_BOOT_SECRETS");
    }

    @Test
    public void testFinalizeNotification() {
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stackView);

        underTest.sendNotification(METADATA, CLOUDBREAK_ROTATE_POLLING);

        verifyEventIsSent("Execute secret [random secret]: small step for a secret, big step for the customer");
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.SALT_BOOT_SECRETS");
    }

    private void verifyEventIsSent(String message) {
        verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.SECRET_ROTATION_STEP, List.of(message));
    }
}