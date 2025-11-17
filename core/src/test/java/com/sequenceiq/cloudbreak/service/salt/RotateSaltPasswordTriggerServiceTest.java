package com.sequenceiq.cloudbreak.service.salt;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordTriggerServiceTest {

    private static final String STACK_CRN = "crn";

    @Mock
    private StackRotationService stackRotationService;

    @Mock
    private SecretRotationValidationService secretRotationValidationService;

    @Mock
    private SecretRotationStepProgressService secretRotationStepProgressService;

    @Mock
    private ReactorFlowManager reactorFlowManager;

    @Mock
    private StackDto stack;

    @InjectMocks
    private RotateSaltPasswordTriggerService underTest;

    @BeforeEach
    void init() {
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        lenient().when(stack.getId()).thenReturn(1L);
    }

    @Test
    void triggerRotateSaltPassword() {
        when(secretRotationValidationService.failedRotationAlreadyHappened(any(), any())).thenReturn(Boolean.FALSE);
        underTest.triggerRotateSaltPassword(stack, RotateSaltPasswordReason.MANUAL);

        verify(stackRotationService).rotateSecrets(STACK_CRN, List.of(SALT_PASSWORD.value()), null, null);
        verify(reactorFlowManager, never()).triggerSaltUpdate(any());
    }

    @Test
    void triggerSaltUpdate() {
        when(secretRotationValidationService.failedRotationAlreadyHappened(any(), any())).thenReturn(Boolean.TRUE);
        when(reactorFlowManager.triggerSaltUpdate(any())).thenReturn(FlowIdentifier.notTriggered());
        doNothing().when(secretRotationStepProgressService).deleteCurrentRotation(any());
        underTest.triggerRotateSaltPassword(stack, RotateSaltPasswordReason.MANUAL);

        verify(stackRotationService, never()).rotateSecrets(any(), any(), any(), any());
        verify(reactorFlowManager).triggerSaltUpdate(any());
        ArgumentCaptor<RotationMetadata> mdCaptor = ArgumentCaptor.forClass(RotationMetadata.class);
        verify(secretRotationStepProgressService).deleteCurrentRotation(mdCaptor.capture());
        assertEquals(SALT_PASSWORD, mdCaptor.getValue().secretType());
        assertEquals(STACK_CRN, mdCaptor.getValue().resourceCrn());
    }

}