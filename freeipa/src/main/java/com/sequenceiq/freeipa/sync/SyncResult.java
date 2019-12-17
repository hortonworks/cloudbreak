package com.sequenceiq.freeipa.sync;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;

public class SyncResult {

    private String message;

    private DetailedStackStatus status;

    private Boolean result;

    public SyncResult(String message, DetailedStackStatus status, Boolean result) {
        this.message = message;
        this.status = status;
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public DetailedStackStatus getStatus() {
        return status;
    }

    public Boolean getResult() {
        return result;
    }
}
