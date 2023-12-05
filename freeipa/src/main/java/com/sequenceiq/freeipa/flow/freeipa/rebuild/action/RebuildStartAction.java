package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBUILD_STARTED_EVENT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

/**
 * TODO
 * Store variables from event which required through the flow, eg FQDN and backup info
 */
@Component("RebuildStartAction")
public class RebuildStartAction extends AbstractRebuildAction<RebuildEvent> {
    protected RebuildStartAction() {
        super(RebuildEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, RebuildEvent payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new StackEvent(REBUILD_STARTED_EVENT.event(), payload.getResourceId()));
    }
}
