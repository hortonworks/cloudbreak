package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("RebuildInstallFreeIpaAction")
public class RebuildInstallFreeIpaAction extends AbstractRebuildAction<ValidateBackupSuccess> {
    protected RebuildInstallFreeIpaAction() {
        super(ValidateBackupSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, ValidateBackupSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Installing FreeIPA");
        InstallFreeIpaServicesRequest request = new InstallFreeIpaServicesRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(ValidateBackupSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new InstallFreeIpaServicesFailed(payload.getResourceId(), ex, ERROR);
    }
}
