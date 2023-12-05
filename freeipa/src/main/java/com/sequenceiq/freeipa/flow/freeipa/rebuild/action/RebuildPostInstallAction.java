package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreSuccess;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;

/**
 * TODO
 * Run postinstall tasks
 *
 * @see FreeIpaUpscaleActions#freeIpaPostInstallAction()
 */
@Component("RebuildPostInstallAction")
public class RebuildPostInstallAction extends AbstractRebuildAction<FreeIpaCleanupAfterRestoreSuccess> {
    protected RebuildPostInstallAction() {
        super(FreeIpaCleanupAfterRestoreSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaCleanupAfterRestoreSuccess payload, Map<Object, Object> variables) throws Exception {
        PostInstallFreeIpaRequest request = new PostInstallFreeIpaRequest(payload.getResourceId(), false);
        sendEvent(context, request.selector(), request);
    }
}
