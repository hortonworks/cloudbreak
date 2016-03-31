package com.sequenceiq.cloudbreak.core.flow2;

import java.util.Map;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;

@Component("FlowFinalizeAction")
public final class FlowFinalizeAction extends AbstractAction<FlowState, FlowEvent, CommonContext, Object> {
    public FlowFinalizeAction() {
        super(Object.class);
    }

    @Override
    protected CommonContext createFlowContext(StateContext<FlowState, FlowEvent> stateContext, Object payload) {
        String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
        return new CommonContext(flowId);
    }

    @Override
    protected void doExecute(CommonContext context, Object payload, Map<Object, Object> variables) {
        sendEvent(context.getFlowId(), Flow2Handler.FLOW_FINAL, payload);
    }

    @Override
    protected Selectable createRequest(CommonContext context) {
        return null;
    }

    @Override
    protected Object getFailurePayload(CommonContext flowContext, Exception ex) {
        return null;
    }
}
