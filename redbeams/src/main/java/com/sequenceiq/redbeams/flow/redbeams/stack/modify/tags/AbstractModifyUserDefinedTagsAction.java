package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags;

import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFlowContextBuilder;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors;

public abstract class AbstractModifyUserDefinedTagsAction<P extends Payload>
        extends AbstractAction<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors, RedbeamsContext, P> {

    @Inject
    private RedbeamsFlowContextBuilder redbeamsFlowContextBuilder;

    protected AbstractModifyUserDefinedTagsAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @PostConstruct
    public void init() {
        super.init();
    }

    @Override
    protected void doExecute(RedbeamsContext context, P payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context);
    }

    @Override
    protected RedbeamsContext createFlowContext(FlowParameters flowParameters, StateContext<ModifyUserDefinedTagsState,
                ModifyUserDefinedTagsStateSelectors> stateContext, P payload) {
        return redbeamsFlowContextBuilder.createRedbeamsContext(flowParameters, payload.getResourceId());
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RedbeamsContext> flowContext, Exception ex) {
        return new ModifyUserDefinedTagsFailedEvent(payload.getResourceId(), ex);
    }
}
