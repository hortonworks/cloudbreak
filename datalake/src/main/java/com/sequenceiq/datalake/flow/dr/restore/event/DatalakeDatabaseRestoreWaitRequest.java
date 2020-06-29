package com.sequenceiq.datalake.flow.dr.restore.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseRestoreWaitRequest extends SdxEvent {

    private final String operationId;

    public DatalakeDatabaseRestoreWaitRequest(Long sdxId, String userId, String operationId) {
        super(sdxId, userId);
        this.operationId = operationId;
    }

    public static DatalakeDatabaseRestoreWaitRequest from(SdxContext context, String operationId) {
        return new DatalakeDatabaseRestoreWaitRequest(context.getSdxId(), context.getUserId(), operationId);
    }

    public String getOperationId() {
        return operationId;
    }
}
