package com.sequenceiq.cloudbreak.service.salt;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordType;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordTriggerServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private RotateSaltPasswordValidator rotateSaltPasswordValidator;

    @Mock
    private StackDto stack;

    @InjectMocks
    private RotateSaltPasswordTriggerService underTest;

    @BeforeEach
    void init() {
        when(stack.getId()).thenReturn(STACK_ID);
    }

    @Test
    void getRotateSaltPasswordTypeFallback() {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);

        underTest.triggerRotateSaltPassword(stack, RotateSaltPasswordReason.MANUAL);

        verify(flowManager).triggerRotateSaltPassword(STACK_ID, RotateSaltPasswordReason.MANUAL, RotateSaltPasswordType.FALLBACK);
    }

    @Test
    void getRotateSaltPasswordTypeSaltBootstrapEndpoint() {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);

        underTest.triggerRotateSaltPassword(stack, RotateSaltPasswordReason.MANUAL);

        verify(flowManager).triggerRotateSaltPassword(STACK_ID, RotateSaltPasswordReason.MANUAL, RotateSaltPasswordType.SALT_BOOTSTRAP_ENDPOINT);
    }

}