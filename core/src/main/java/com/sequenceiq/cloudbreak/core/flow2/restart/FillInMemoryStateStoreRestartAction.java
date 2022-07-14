package com.sequenceiq.cloudbreak.core.flow2.restart;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("FillInMemoryStateStoreRestartAction")
public class FillInMemoryStateStoreRestartAction extends DefaultRestartAction {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Override
    public void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload) {
        Payload stackPayload = (Payload) payload;
        StackView stack = stackDtoService.getStackViewById(stackPayload.getResourceId());
        restart(flowParameters, flowChainId, event, payload, stack);
    }

    protected void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload, StackView stack) {
        InMemoryStateStore.putStack(stack.getId(), statusToPollGroupConverter.convert(stack.getStatus()));
        if (stack.getClusterId() != null) {
            InMemoryStateStore.putCluster(stack.getClusterId(), statusToPollGroupConverter.convert(stack.getStatus()));
        }
        MDCBuilder.buildMdcContext(stack);
        super.restart(flowParameters, flowChainId, event, payload);
    }
}
