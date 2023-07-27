package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@ExtendWith(MockitoExtension.class)
class StackSecretRotationNotificationServiceTest {

    private static final Long STACK_ID = 1L;

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
                .thenReturn("Secret type")
                .thenReturn("Secret step");
    }

    @Test
    public void testRotationNotification() {
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stackView);

        underTest.sendNotification("crn", DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS, CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING,
                RotationFlowExecutionType.ROTATE);

        verifyEventIsSent("Rotation(Secret type) Secret step");
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS");
    }

    @Test
    public void testRollbackNotification() {
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stackView);

        underTest.sendNotification("crn", DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS, CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING,
                RotationFlowExecutionType.ROLLBACK);

        verifyEventIsSent("Rollback(Secret type) Secret step");
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS");
    }

    @Test
    public void testFinalizeNotification() {
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stackView);

        underTest.sendNotification("crn", DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS, CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING,
                RotationFlowExecutionType.FINALIZE);

        verifyEventIsSent("Finalization(Secret type) Secret step");
        verify(cloudbreakMessagesService).getMessage("DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS");
    }

    private void verifyEventIsSent(String message) {
        verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.SECRET_ROTATION_STEP, List.of(message));
    }
}