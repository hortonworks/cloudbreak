package com.sequenceiq.flow.api.model.operation;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum OperationProgressStatus {
    UNKNOWN, RUNNING, FINISHED, CANCELLED, FAILED;

    @JsonIgnore
    public boolean isCompleted() {
        return StringUtils.equalsAny(name(), FINISHED.name(), CANCELLED.name(), FAILED.name());
    }
}
