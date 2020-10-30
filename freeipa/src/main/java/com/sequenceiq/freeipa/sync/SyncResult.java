package com.sequenceiq.freeipa.sync;

import java.util.Map;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

public class SyncResult {

    private String message;

    private DetailedStackStatus status;

    private Map<InstanceMetaData, DetailedStackStatus> instanceStatusMap;

    public SyncResult(String message, DetailedStackStatus status, Map<InstanceMetaData, DetailedStackStatus> instanceStatusMap) {
        this.message = message;
        this.status = status;
        this.instanceStatusMap = instanceStatusMap;
    }

    public String getMessage() {
        return message;
    }

    public DetailedStackStatus getStatus() {
        return status;
    }

    public Map<InstanceMetaData, DetailedStackStatus> getInstanceStatusMap() {
        return instanceStatusMap;
    }
}
