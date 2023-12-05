package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

/**
 * TODO
 */
@Component("RebuildFailedAction")
public class RebuildFailedAction extends AbstractRebuildAction<RebuildFailureEvent> {
    protected RebuildFailedAction() {
        super(RebuildFailureEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, RebuildFailureEvent payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new StackEvent(FreeIpaRebuildFlowEvent.REBUILD_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
    }
}
