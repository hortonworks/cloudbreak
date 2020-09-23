package com.sequenceiq.freeipa.sync;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;

public class SyncResult {

    private String message;

    private DetailedStackStatus status;

    public SyncResult(String message, DetailedStackStatus status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public DetailedStackStatus getStatus() {
        return status;
    }
}
