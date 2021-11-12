package com.sequenceiq.cloudbreak.core.flow2.restart;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("FillInMemoryStateStoreRestartAction")
public class FillInMemoryStateStoreRestartAction extends DefaultRestartAction {

    @Inject
    private StackService stackService;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Override
    public void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload) {
        Payload stackPayload = (Payload) payload;
        Stack stack = stackService.getByIdWithListsInTransaction(stackPayload.getResourceId());
        restart(flowParameters, flowChainId, event, payload, stack);
    }

    protected void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload, Stack stack) {
        InMemoryStateStore.putStack(stack.getId(), statusToPollGroupConverter.convert(stack.getStatus()));
        if (stack.getCluster() != null) {
            InMemoryStateStore.putCluster(stack.getCluster().getId(), statusToPollGroupConverter.convert(stack.getStatus()));
        }
        MDCBuilder.buildMdcContext(stack);
        super.restart(flowParameters, flowChainId, event, payload);
    }
}
