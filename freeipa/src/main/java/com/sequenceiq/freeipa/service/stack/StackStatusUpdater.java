package com.sequenceiq.freeipa.service.stack;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.message.StackStatusMessageTransformator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;

@Service
public class StackStatusUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusUpdater.class);

    @Inject
    private StackStatusMessageTransformator stackStatusMessageTransformator;

    @Inject
    private ServiceStatusRawMessageTransformer serviceStatusRawMessageTransformer;

    @Inject
    private StackService stackService;

    public Stack update(Stack stack, DetailedStackStatus newDetailedStatus, String rawNewStatusReason) {
        StackStatus stackStatus = stack.getStackStatus();
        if (!Status.DELETE_COMPLETED.equals(stackStatus.getStatus())) {
            rawNewStatusReason = serviceStatusRawMessageTransformer.transformMessage(rawNewStatusReason, stack.getTunnel());
            String transformedNewStatusReason = stackStatusMessageTransformator.transformMessage(rawNewStatusReason);
            if (isStatusChanged(stack, newDetailedStatus, transformedNewStatusReason)) {
                stack = handleStatusChange(stack, stackStatus, newDetailedStatus, transformedNewStatusReason);
            } else {
                LOGGER.debug("Statuses are the same, it will not update");
                updateInMemoryStoreIfStackIsMissing(stack, newDetailedStatus.getStatus());
            }
        }
        return stack;
    }

    private Stack updateStatus(Stack stackOriginal, DetailedStackStatus newDetailedStatus, String newStatusReason, StackStatus stackStatus) {
        Stack stack = stackService.getStackById(stackOriginal.getId());
        Status newStatus = newDetailedStatus.getStatus();
        LOGGER.debug("Updated: status from {} to {} - detailed status from {} to {} - reason from {} to {}",
                stackStatus.getStatus(), newStatus, stackStatus.getDetailedStackStatus(), newDetailedStatus,
                stackStatus.getStatusReason(), newStatusReason);
        stack.setStackStatus(new StackStatus(stack, newStatus, newStatusReason, newDetailedStatus));
        stack = stackService.save(stack);
        return stack;
    }

    private void updateInMemoryStoreIfStackIsMissing(Stack stack, Status newStatus) {
        if (InMemoryStateStore.getStack(stack.getId()) == null) {
            LOGGER.debug("Although status hasn't changed, the stack is missing from 'InMemoryStateStore'. Updating 'InMemoryStateStore'");
            updateInMemoryStore(stack, newStatus);
        }
    }

    private Stack handleStatusChange(Stack stack, StackStatus stackStatus, DetailedStackStatus newDetailedStatus, String newStatusReason) {
        stack = updateStatus(stack, newDetailedStatus, newStatusReason, stackStatus);
        updateInMemoryStore(stack, newDetailedStatus.getStatus());
        return stack;
    }

    private void updateInMemoryStore(Stack stack, Status newStatus) {
        if (newStatus.isRemovableStatus()) {
            InMemoryStateStore.deleteStack(stack.getId());
        } else {
            PollGroup pollGroup = Status.DELETE_COMPLETED.equals(newStatus) ? PollGroup.CANCELLED : PollGroup.POLLABLE;
            InMemoryStateStore.putStack(stack.getId(), pollGroup);
        }
    }

    private boolean isStatusChanged(Stack stack, DetailedStackStatus newDetailedStatus, String newStatusReason) {
        return newDetailedStatus.getStatus() != stack.getStackStatus().getStatus()
                || !newDetailedStatus.equals(stack.getStackStatus().getDetailedStackStatus())
                || !Objects.equals(newStatusReason, stack.getStackStatus().getStatusReason());
    }

}
