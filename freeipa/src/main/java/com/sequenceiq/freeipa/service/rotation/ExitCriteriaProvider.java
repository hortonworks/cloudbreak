package com.sequenceiq.freeipa.service.rotation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;

@Component
public class ExitCriteriaProvider {

    public ExitCriteriaModel get(Stack stack) {
        PollGroup pollGroup = Status.DELETE_COMPLETED == stack.getStackStatus().getStatus() ? PollGroup.CANCELLED : PollGroup.POLLABLE;
        InMemoryStateStore.putStack(stack.getId(), pollGroup);
        return new StackBasedExitCriteriaModel(stack.getId());
    }
}
