package com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet;

import com.sequenceiq.cloudbreak.cloud.event.Payload;

public class UpdateSubnetFailedPayload implements Payload {

    private final Long stackId;
    private final Exception exception;

    public UpdateSubnetFailedPayload(Long stackId, Exception exception) {
        this.stackId = stackId;
        this.exception = exception;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }

    public Exception getException() {
        return exception;
    }
}
