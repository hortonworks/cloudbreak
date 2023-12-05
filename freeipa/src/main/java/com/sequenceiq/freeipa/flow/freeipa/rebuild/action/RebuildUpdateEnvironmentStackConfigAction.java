package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

/**
 * TODO
 * Kerberos config must have the new FreeIPA IP
 *
 * @see FreeIpaUpscaleActions#updateEnvironmentStackConfigAction()
 */
@Component("RebuildUpdateEnvironmentStackConfigAction")
public class RebuildUpdateEnvironmentStackConfigAction extends AbstractRebuildAction<StackEvent> {
    protected RebuildUpdateEnvironmentStackConfigAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new StackEvent(UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
