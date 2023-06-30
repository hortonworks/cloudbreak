package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_STARTED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class StackSecretRotationStatusServiceTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String STATUS_REASON = "reason";

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private StackSecretRotationStatusService underTest;

    @Test
    void testRotationStarted() {
        underTest.rotationStarted(RESOURCE_CRN);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_STARTED), isNull());
    }

    @Test
    void testRotationFinished() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(SECRET_ROTATION_STARTED));
        underTest.rotationFinished(RESOURCE_CRN);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FINISHED), isNull());
    }

    @Test
    void testRotationFinishedShouldSkipStatusUpdateWhenStatusIsAlreadySecretRotationFailed() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(SECRET_ROTATION_FAILED));
        underTest.rotationFinished(RESOURCE_CRN);
        verify(stackUpdater, never()).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FINISHED), isNull());
    }

    @Test
    void testRotationFailed() {
        underTest.rotationFailed(RESOURCE_CRN, STATUS_REASON);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FAILED), eq(STATUS_REASON));
    }

    private Stack stack(DetailedStackStatus detailedStackStatus) {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus));
        return stack;
    }
}