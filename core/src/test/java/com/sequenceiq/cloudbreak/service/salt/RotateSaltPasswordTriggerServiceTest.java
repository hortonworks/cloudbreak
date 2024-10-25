package com.sequenceiq.cloudbreak.service.salt;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordTriggerServiceTest {

    private static final String STACK_CRN = "crn";

    @Mock
    private StackRotationService stackRotationService;

    @Mock
    private StackDto stack;

    @InjectMocks
    private RotateSaltPasswordTriggerService underTest;

    @BeforeEach
    void init() {
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
    }

    @Test
    void triggerRotateSaltPassword() {
        underTest.triggerRotateSaltPassword(stack, RotateSaltPasswordReason.MANUAL);

        verify(stackRotationService).rotateSecrets(STACK_CRN, List.of(CloudbreakSecretType.SALT_PASSWORD.value()), null, null);
    }

}