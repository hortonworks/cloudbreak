package com.sequenceiq.redbeams.flow.redbeams.upgrade;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFlowContextBuilder;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsUpgradeFailedEvent;

public abstract class AbstractRedbeamsUpgradeAction<P extends Payload> extends AbstractAction<RedbeamsUpgradeState, RedbeamsUpgradeEvent, RedbeamsContext, P> {

    @Inject
    private RedbeamsFlowContextBuilder redbeamsFlowContextBuilder;

    protected AbstractRedbeamsUpgradeAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected RedbeamsContext createFlowContext(FlowParameters flowParameters, StateContext<RedbeamsUpgradeState, RedbeamsUpgradeEvent> stateContext,
            P payload) {
        return redbeamsFlowContextBuilder.createRedbeamsContext(flowParameters, payload.getResourceId());
    }

    @Override
    protected void doExecute(RedbeamsContext context, P payload, Map<Object, Object> variables) {
        sendEvent(context);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RedbeamsContext> flowContext, Exception ex) {
        return new RedbeamsUpgradeFailedEvent(payload.getResourceId(), ex);
    }

}
