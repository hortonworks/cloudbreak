package com.sequenceiq.datalake.flow.dr.restore.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRestoreSuccessEvent extends SdxEvent {
    private final String operationId;

    public DatalakeRestoreSuccessEvent(Long sdxId, String userId, String operationId) {
        super(sdxId, userId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

}
