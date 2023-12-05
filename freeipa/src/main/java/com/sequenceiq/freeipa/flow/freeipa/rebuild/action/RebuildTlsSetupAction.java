package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.TLS_SETUP_FINISHED_EVENT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

/**
 * TODO
 * Save extra info from provider regarding instance
 *
 * @see FreeIpaUpscaleActions#tlsSetupAction()
 */
@Component("RebuildTlsSetupAction")
public class RebuildTlsSetupAction extends AbstractRebuildAction<StackEvent> {
    protected RebuildTlsSetupAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new StackEvent(TLS_SETUP_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
