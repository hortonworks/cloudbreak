package com.sequenceiq.cloudbreak.core.flow2;

import java.util.Map;
import java.util.Optional;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;

@Component("FlowFinalizeAction")
public final class FlowFinalizeAction extends AbstractAction<FlowState, FlowEvent, CommonContext, Payload> {
    public FlowFinalizeAction() {
        super(Payload.class);
    }

    @Override
    protected CommonContext createFlowContext(String flowId, StateContext<FlowState, FlowEvent> stateContext, Payload payload) {
        return new CommonContext(flowId);
    }

    @Override
    protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
        sendEvent(context.getFlowId(), FlowConstants.FLOW_FINAL, payload);
    }

    @Override
    protected Selectable createRequest(CommonContext context) {
        return null;
    }

    @Override
    protected Object getFailurePayload(Payload payload, Optional<CommonContext> flowContext, Exception ex) {
        return null;
    }
}
