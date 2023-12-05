package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageSuccess;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

/**
 * TODO
 * Validate the provided backup is downloadable etc
 */
@Component("RebuildValidateBackupAction")
public class RebuildValidateBackupAction extends AbstractRebuildAction<ValidateCloudStorageSuccess> {
    protected RebuildValidateBackupAction() {
        super(ValidateCloudStorageSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, ValidateCloudStorageSuccess payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new ValidateBackupRequest(payload.getResourceId(), "", "", ""));
    }
}
