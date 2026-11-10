package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerRequest;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFlowContextBuilder;

public abstract class AbstractChangeInstanceTypeAction<P extends Payload>
        extends AbstractAction<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors, RedbeamsContext, P> {

    @Inject
    private RedbeamsFlowContextBuilder redbeamsFlowContextBuilder;

    protected AbstractChangeInstanceTypeAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected RedbeamsContext createFlowContext(FlowParameters flowParameters,
            StateContext<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors> stateContext, P payload) {
        return redbeamsFlowContextBuilder.createRedbeamsContext(flowParameters, payload.getResourceId());
    }

    @Override
    protected void doExecute(RedbeamsContext context, P payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RedbeamsContext> flowContext, Exception ex) {
        return new ChangeInstanceTypeFailedEvent(payload.getResourceId(), ex);
    }

    protected void sendHandlerRequest(RedbeamsContext context, ChangeInstanceTypeHandlerSelectors handlerSelector, ChangeInstanceTypeEvent payload) {
        sendEvent(context, new ChangeInstanceTypeHandlerRequest(handlerSelector.event(), context.getCloudContext(), context.getCloudCredential(),
                context.getDatabaseStack(), context.getDBStack().getEnvironmentId(), payload.getTargetInstanceType(), payload.getLiveState()));
    }
}
