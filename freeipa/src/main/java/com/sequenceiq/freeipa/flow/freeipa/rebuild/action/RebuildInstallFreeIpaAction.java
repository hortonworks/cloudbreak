package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupSuccess;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;

/**
 * TODO
 * Install FreeIPA
 *
 * @see FreeIpaUpscaleActions#installFreeIpaAction()
 */
@Component("RebuildInstallFreeIpaAction")
public class RebuildInstallFreeIpaAction extends AbstractRebuildAction<ValidateBackupSuccess> {
    protected RebuildInstallFreeIpaAction() {
        super(ValidateBackupSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, ValidateBackupSuccess payload, Map<Object, Object> variables) throws Exception {
        InstallFreeIpaServicesRequest request = new InstallFreeIpaServicesRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }
}
