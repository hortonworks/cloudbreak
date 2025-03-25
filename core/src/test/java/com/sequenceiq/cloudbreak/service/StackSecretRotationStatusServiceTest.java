package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FINALIZE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FINALIZE_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FINALIZE_STARTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_ROLLBACK_STARTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECRET_ROTATION_STARTED;
import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class StackSecretRotationStatusServiceTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final SecretType SECRET_TYPE = mock(SecretType.class);

    private static final String SECRET_TYPE_NAME = "secretType";

    private static final String REASON = "reason";

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private StackSecretRotationStatusService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(secretRotationNotificationService.getMessage(any(), any())).thenReturn(SECRET_TYPE_NAME);
    }

    @Test
    void rotationStartedShouldUpdateStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(AVAILABLE));
        underTest.rotationStarted(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_STARTED), anyString());
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(1L, UPDATE_IN_PROGRESS.name(),
                ResourceEvent.SECRET_ROTATION_IN_PROGRESS, List.of(SECRET_TYPE_NAME));
    }

    @Test
    void rotationFinishedShouldUpdateStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(AVAILABLE));
        underTest.rotationFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FINISHED), anyString());
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(1L, UPDATE_IN_PROGRESS.name(),
                ResourceEvent.SECRET_ROTATION_FINISHED, List.of(SECRET_TYPE_NAME));
    }

    @Test
    void rotationFailedShouldSkipStatusUpdateWhenStatusIsAlreadySecretRotationRollbackFailedStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(SECRET_ROTATION_ROLLBACK_FAILED));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verifyNoInteractions(stackUpdater, cloudbreakEventService);
    }

    @Test
    void rotationFailedShouldSkipStatusUpdateWhenStatusIsAlreadySecretRotationFinalizeFailedStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(SECRET_ROTATION_FINALIZE_FAILED));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verifyNoInteractions(stackUpdater, cloudbreakEventService);
    }

    @Test
    void rotationFailedShouldUpdateStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(SECRET_ROTATION_STARTED));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FAILED),
                eq("Secret rotation failed: secretType"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(1L, UPDATE_FAILED.name(),
                ResourceEvent.SECRET_ROTATION_FAILED, List.of(SECRET_TYPE_NAME, REASON));
    }

    @Test
    void rollbackStartedShouldUpdateStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(AVAILABLE));
        underTest.rollbackStarted(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_ROLLBACK_STARTED),
                eq("Secret rotation rollback started: secretType"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(1L, UPDATE_FAILED.name(),
                ResourceEvent.SECRET_ROTATION_ROLLBACK_IN_PROGRESS, List.of(SECRET_TYPE_NAME, REASON));
    }

    @Test
    void rollbackFinishedShouldUpdateStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(AVAILABLE));
        underTest.rollbackFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_ROLLBACK_FINISHED), anyString());
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(1L, UPDATE_IN_PROGRESS.name(),
                ResourceEvent.SECRET_ROTATION_ROLLBACK_FINISHED, List.of(SECRET_TYPE_NAME));
    }

    @Test
    void rollbackFailedShouldUpdateStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(AVAILABLE));
        underTest.rollbackFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_ROLLBACK_FAILED),
                eq("Secret rotation rollback failed: secretType"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(1L, UPDATE_FAILED.name(),
                ResourceEvent.SECRET_ROTATION_ROLLBACK_FAILED, List.of(SECRET_TYPE_NAME, REASON));
    }

    @Test
    void finalizeStartedShouldUpdateStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(AVAILABLE));
        underTest.finalizeStarted(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FINALIZE_STARTED), anyString());
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(1L, UPDATE_IN_PROGRESS.name(),
                ResourceEvent.SECRET_ROTATION_FINALIZE_IN_PROGRESS, List.of(SECRET_TYPE_NAME));
    }

    @Test
    void finalizeFinishedShouldUpdateStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(AVAILABLE));
        underTest.finalizeFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FINALIZE_FINISHED), anyString());
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(1L, UPDATE_IN_PROGRESS.name(),
                ResourceEvent.SECRET_ROTATION_FINALIZE_FINISHED, List.of(SECRET_TYPE_NAME));
    }

    @Test
    void finalizeFailedShouldUpdateStatus() {
        when(stackDtoService.getStackViewByCrn(eq(RESOURCE_CRN))).thenReturn(stack(AVAILABLE));
        underTest.finalizeFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(stackUpdater, times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(SECRET_ROTATION_FINALIZE_FAILED),
                eq("Secret rotation finalize failed: secretType"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(1L, UPDATE_FAILED.name(),
                ResourceEvent.SECRET_ROTATION_FINALIZE_FAILED, List.of(SECRET_TYPE_NAME, REASON));
    }

    private Stack stack(DetailedStackStatus detailedStackStatus) {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setType(StackType.WORKLOAD);
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus));
        return stack;
    }
}