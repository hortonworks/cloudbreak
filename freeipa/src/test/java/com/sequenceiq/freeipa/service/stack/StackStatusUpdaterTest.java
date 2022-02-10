package com.sequenceiq.freeipa.service.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.message.StackStatusMessageTransformator;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;

@ExtendWith(MockitoExtension.class)
public class StackStatusUpdaterTest {

    private static final long STACK_ID = 1234L;

    private static final String REASON = "myReason";

    private static final String TRANSFORMED_REASON = "transform2";

    @Mock
    private StackService stackService;

    @Mock
    private StackStatusMessageTransformator stackStatusMessageTransformator;

    @Mock
    private ServiceStatusRawMessageTransformer serviceStatusRawMessageTransformer;

    @Mock
    private StackStatusUpdater stackStatusUpdater;

    @InjectMocks
    private StackStatusUpdater underTest;

    @Test
    void testDoUpdateStackStatusWhenNoStatusChange() {
        Stack stack = getStack(DetailedStackStatus.AVAILABLE, TRANSFORMED_REASON);
        DetailedStackStatus newStackStatus = DetailedStackStatus.AVAILABLE;
        InMemoryStateStore.putStack(stack.getId(), PollGroup.POLLABLE);
        when(serviceStatusRawMessageTransformer.transformMessage(eq(REASON), any(Tunnel.class))).thenReturn("transform1");
        when(stackStatusMessageTransformator.transformMessage("transform1")).thenReturn(TRANSFORMED_REASON);

        underTest.update(stack, newStackStatus, REASON);

        verify(stackService, never()).save(stack);
        assertThat(InMemoryStateStore.getAllStackId()).contains(STACK_ID);
    }

    @Test
    void testDoUpdateStackStatusWhenStatusIsDeleteCompleted() {
        Stack stack = getStack(DetailedStackStatus.DELETE_COMPLETED, "deleteReason");
        DetailedStackStatus newStackStatus = DetailedStackStatus.AVAILABLE;

        underTest.update(stack, newStackStatus, REASON);

        verify(serviceStatusRawMessageTransformer, never()).transformMessage(any(), any());
        verify(stackStatusMessageTransformator, never()).transformMessage(any());
        verify(stackService, never()).save(stack);
        assertThat(InMemoryStateStore.getStack(STACK_ID)).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = DetailedStackStatus.class, names = {"AVAILABLE", "DELETE_FAILED", "DELETE_COMPLETED", "STOPPED", "START_FAILED",
            "STOP_FAILED", "REPAIR_FAILED", "UPSCALE_FAILED", "DOWNSCALE_FAILED", "PROVISIONED", "SALT_STATE_UPDATE_FAILED", "REPAIR_COMPLETED",
            "DOWNSCALE_COMPLETED", "UPSCALE_COMPLETED", "STARTED", "PROVISION_FAILED"})
    void testDoUpdateStackStatusWhenRemovableState(DetailedStackStatus detailedStackStatus) {
        Stack stack = getStack(DetailedStackStatus.CREATING_INFRASTRUCTURE, "oldReason");
        InMemoryStateStore.putStack(stack.getId(), PollGroup.POLLABLE);
        when(serviceStatusRawMessageTransformer.transformMessage(eq(REASON), any(Tunnel.class))).thenReturn("transform1");
        when(stackStatusMessageTransformator.transformMessage("transform1")).thenReturn(TRANSFORMED_REASON);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(stackService.save(stack)).thenReturn(stack);

        underTest.update(stack, detailedStackStatus, REASON);

        assertNewStatusIsSaved(detailedStackStatus);
        assertThat(InMemoryStateStore.getStack(STACK_ID)).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = DetailedStackStatus.class, names = {"AVAILABLE", "DELETE_FAILED", "DELETE_COMPLETED", "STOPPED", "START_FAILED",
            "STOP_FAILED", "REPAIR_FAILED", "UPSCALE_FAILED", "DOWNSCALE_FAILED", "PROVISIONED", "SALT_STATE_UPDATE_FAILED", "REPAIR_COMPLETED",
            "DOWNSCALE_COMPLETED", "UPSCALE_COMPLETED", "STARTED", "PROVISION_FAILED"}, mode = EnumSource.Mode.EXCLUDE)
    void testDoUpdateStackStatusWhenNotRemovableState(DetailedStackStatus detailedStackStatus) {
        Stack stack = getStack(DetailedStackStatus.AVAILABLE, "oldReason");
        when(serviceStatusRawMessageTransformer.transformMessage(eq(REASON), any(Tunnel.class))).thenReturn("transform1");
        when(stackStatusMessageTransformator.transformMessage("transform1")).thenReturn(TRANSFORMED_REASON);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(stackService.save(stack)).thenReturn(stack);

        underTest.update(stack, detailedStackStatus, REASON);

        assertNewStatusIsSaved(detailedStackStatus);
        assertThat(InMemoryStateStore.getAllStackId()).contains(STACK_ID);
    }

    private void assertNewStatusIsSaved(DetailedStackStatus newDetailedStackStatus) {
        ArgumentCaptor<Stack> captor = ArgumentCaptor.forClass(Stack.class);
        verify(stackService).save(captor.capture());
        StackStatus savedStackStatus = captor.getValue().getStackStatus();
        assertEquals(savedStackStatus.getDetailedStackStatus(), newDetailedStackStatus);
        assertEquals(TRANSFORMED_REASON, savedStackStatus.getStatusReason());
    }

    private Stack getStack(DetailedStackStatus originalStatus, String statusReason) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setDetailedStackStatus(originalStatus);
        stackStatus.setStatus(originalStatus.getStatus());
        stackStatus.setStatusReason(statusReason);
        stack.setStackStatus(stackStatus);
        return stack;
    }

}
