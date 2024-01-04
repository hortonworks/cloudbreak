package com.sequenceiq.freeipa.flow;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.RestartContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component("FillInMemoryStateStoreRestartAction")
public class FillInMemoryStateStoreRestartAction extends DefaultRestartAction {

    @Inject
    private StackService stackService;

    @Override
    public void doBeforeRestart(RestartContext restartContext, Object payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(restartContext.getResourceId());
        PollGroup pollGroup = Status.DELETE_COMPLETED == stack.getStackStatus().getStatus() ? PollGroup.CANCELLED : PollGroup.POLLABLE;
        InMemoryStateStore.putStack(stack.getId(), pollGroup);
        MDCBuilder.buildMdcContext(stack);
    }
}
