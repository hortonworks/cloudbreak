package com.sequenceiq.datalake.flow.dr.restore.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseRestoreSuccessEvent extends SdxEvent {
    private final String operationId;

    public DatalakeDatabaseRestoreSuccessEvent(Long sdxId, String userId, String operationId) {
        super(sdxId, userId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

}
