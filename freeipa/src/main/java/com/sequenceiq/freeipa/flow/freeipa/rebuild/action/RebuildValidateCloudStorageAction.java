package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("RebuildValidateCloudStorageAction")
public class RebuildValidateCloudStorageAction extends AbstractRebuildAction<OrchestratorConfigSuccess> {
    protected RebuildValidateCloudStorageAction() {
        super(OrchestratorConfigSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, OrchestratorConfigSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Validating cloud storage");
        ValidateCloudStorageRequest request = new ValidateCloudStorageRequest(payload.getResourceId());
        sendEvent(context, request);
    }

    @Override
    protected Object getFailurePayload(OrchestratorConfigSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new ValidateCloudStorageFailed(payload.getResourceId(), ex, VALIDATION);
    }
}