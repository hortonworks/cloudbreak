package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigSuccess;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;

/**
 * TODO
 * Validate cloud storage for backup as during normal upscale
 *
 * @see FreeIpaUpscaleActions#validateFreeIpaCloudStorage() ()
 */
@Component("RebuildValidateCloudStorageAction")
public class RebuildValidateCloudStorageAction extends AbstractRebuildAction<OrchestratorConfigSuccess> {
    protected RebuildValidateCloudStorageAction() {
        super(OrchestratorConfigSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, OrchestratorConfigSuccess payload, Map<Object, Object> variables) throws Exception {
        ValidateCloudStorageRequest request = new ValidateCloudStorageRequest(payload.getResourceId());
        sendEvent(context, request);
    }
}
