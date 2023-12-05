package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

/**
 * TODO
 * Restore backup
 */
@Component("RebuildRestoreFreeIpaAction")
public class RebuildRestoreFreeIpaAction extends AbstractRebuildAction<InstallFreeIpaServicesSuccess> {
    protected RebuildRestoreFreeIpaAction() {
        super(InstallFreeIpaServicesSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, InstallFreeIpaServicesSuccess payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new FreeIpaRestoreRequest(payload.getResourceId(), "", "", ""));
    }
}
