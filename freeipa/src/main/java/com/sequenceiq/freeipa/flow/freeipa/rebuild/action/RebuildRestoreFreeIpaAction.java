package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("RebuildRestoreFreeIpaAction")
public class RebuildRestoreFreeIpaAction extends AbstractRebuildAction<InstallFreeIpaServicesSuccess> {
    protected RebuildRestoreFreeIpaAction() {
        super(InstallFreeIpaServicesSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, InstallFreeIpaServicesSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Restoring FreeIPA from backup");
        sendEvent(context, new FreeIpaRestoreRequest(payload.getResourceId()));
    }

    @Override
    protected Object getFailurePayload(InstallFreeIpaServicesSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaRestoreFailed(payload.getResourceId(), ex, ERROR);
    }
}
