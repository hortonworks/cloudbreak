package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("RebuildPostInstallAction")
public class RebuildPostInstallAction extends AbstractRebuildAction<FreeIpaCleanupAfterRestoreSuccess> {
    protected RebuildPostInstallAction() {
        super(FreeIpaCleanupAfterRestoreSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaCleanupAfterRestoreSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "FreeIPA Post Installation");
        PostInstallFreeIpaRequest request = new PostInstallFreeIpaRequest(payload.getResourceId(), false);
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FreeIpaCleanupAfterRestoreSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new PostInstallFreeIpaFailed(payload.getResourceId(), ex, ERROR);
    }
}
