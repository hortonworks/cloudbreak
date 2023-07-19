package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FINALIZE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FINALIZE_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FINALIZE_STARTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_ROLLBACK_STARTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_STARTED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class StackSecretRotationStatusServiceTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final SecretType SECRET_TYPE = mock(SecretType.class);

    private static final String REASON = "reason";

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private StackSecretRotationStatusService underTest;

    @Test
    void rotationStartedShouldUpdateStatus() {
        underTest.rotationStarted(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_STARTED), anyString());
    }

    @Test
    void rotationFinishedShouldUpdateStatus() {
        underTest.rotationFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FINISHED), anyString());
    }

    @Test
    void rotationFailedShouldSkipStatusUpdateWhenStatusIsAlreadySecretRotationRollbackFailedStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(SECRET_ROTATION_ROLLBACK_FAILED));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(stackUpdater, never()).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FAILED), contains(REASON));
    }

    @Test
    void rotationFailedShouldSkipStatusUpdateWhenStatusIsAlreadySecretRotationFinalizeFailedStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(SECRET_ROTATION_FINALIZE_FAILED));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(stackUpdater, never()).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FAILED), contains(REASON));
    }

    @Test
    void rotationFailedShouldUpdateStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(SECRET_ROTATION_STARTED));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FAILED), contains(REASON));
    }

    @Test
    void rollbackStartedShouldUpdateStatus() {
        underTest.rollbackStarted(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_ROLLBACK_STARTED), anyString());
    }

    @Test
    void rollbackFinishedShouldUpdateStatus() {
        underTest.rollbackFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_ROLLBACK_FINISHED), anyString());
    }

    @Test
    void rollbackFailedShouldUpdateStatus() {
        underTest.rollbackFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_ROLLBACK_FAILED), contains(REASON));
    }

    @Test
    void finalizeStartedShouldUpdateStatus() {
        underTest.finalizeStarted(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FINALIZE_STARTED), anyString());
    }

    @Test
    void finalizeFinishedShouldUpdateStatus() {
        underTest.finalizeFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FINALIZE_FINISHED), anyString());
    }

    @Test
    void finalizeFailedShouldUpdateStatus() {
        underTest.finalizeFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FINALIZE_FAILED), contains(REASON));
    }

    private Stack stack(DetailedStackStatus detailedStackStatus) {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus));
        return stack;
    }
}