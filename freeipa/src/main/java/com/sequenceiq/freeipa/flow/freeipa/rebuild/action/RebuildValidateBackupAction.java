package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageSuccess;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("RebuildValidateBackupAction")
public class RebuildValidateBackupAction extends AbstractRebuildAction<ValidateCloudStorageSuccess> {
    protected RebuildValidateBackupAction() {
        super(ValidateCloudStorageSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, ValidateCloudStorageSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Downloading and validating backup");
        sendEvent(context, new ValidateBackupRequest(payload.getResourceId(), getFullBackupStorageLocation(variables),
                getDataBackupStorageLocation(variables)));
    }

    @Override
    protected Object getFailurePayload(ValidateCloudStorageSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new ValidateBackupFailed(payload.getResourceId(), ex, VALIDATION);
    }
}
