package com.sequenceiq.datalake.flow.dr.backup.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeFullBackupWaitRequest extends SdxEvent {

    private final String operationId;

    public DatalakeFullBackupWaitRequest(Long sdxId, String userId, String operationId) {
        super(sdxId, userId);
        this.operationId = operationId;
    }

    public static DatalakeFullBackupWaitRequest from(SdxContext context, String operationId) {
        return new DatalakeFullBackupWaitRequest(context.getSdxId(), context.getUserId(), operationId);
    }

    public String getOperationId() {
        return operationId;
    }

}
