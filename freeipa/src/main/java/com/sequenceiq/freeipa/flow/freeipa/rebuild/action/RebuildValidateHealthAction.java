package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaSuccess;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;

/**
 * TODO
 * Run healthcheck
 *
 * @see FreeIpaUpscaleActions#validateNewInstanceAction
 */
@Component("RebuildValidateHealthAction")
public class RebuildValidateHealthAction extends AbstractRebuildAction<PostInstallFreeIpaSuccess> {
    protected RebuildValidateHealthAction() {
        super(PostInstallFreeIpaSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, PostInstallFreeIpaSuccess payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new RebuildValidateHealthRequest(payload.getResourceId()));
    }
}
