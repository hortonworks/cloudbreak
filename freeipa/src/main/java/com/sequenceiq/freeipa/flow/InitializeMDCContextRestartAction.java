package com.sequenceiq.freeipa.flow;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component("InitializeMDCContextRestartAction")
public class InitializeMDCContextRestartAction extends DefaultRestartAction {

    @Inject
    private StackService stackService;

    @Override
    public void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload) {
        Payload stackPayload = (Payload) payload;
        Stack stack = stackService.getStackById(stackPayload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        super.restart(flowParameters, flowChainId, event, payload);
    }
}
