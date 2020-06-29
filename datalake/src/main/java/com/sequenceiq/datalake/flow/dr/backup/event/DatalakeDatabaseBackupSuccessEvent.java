package com.sequenceiq.datalake.flow.dr.backup.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseBackupSuccessEvent extends SdxEvent {
    private final String operationId;

    public DatalakeDatabaseBackupSuccessEvent(Long sdxId, String userId, String operationId) {
        super(sdxId, userId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

}
