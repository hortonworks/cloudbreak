package com.sequenceiq.freeipa.sync;


import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;

public class ProviderSyncResult {

    private String message;

    private InstanceStatus status;

    private Boolean result;

    private String instanceId;

    public ProviderSyncResult(String message, InstanceStatus status, Boolean result, String instanceId) {
        this.message = message;
        this.status = status;
        this.result = result;
        this.instanceId = instanceId;
    }

    public String getMessage() {
        return message;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public Boolean getResult() {
        return result;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
