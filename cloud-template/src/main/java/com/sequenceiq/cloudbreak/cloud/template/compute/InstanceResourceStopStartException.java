package com.sequenceiq.cloudbreak.cloud.template.compute;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

public class InstanceResourceStopStartException extends Exception {

    private final CloudInstance instance;

    private final boolean startOperation;

    public InstanceResourceStopStartException(String message, Throwable cause, CloudInstance instance, boolean startOperation) {
        super(message, cause);
        this.instance = instance;
        this.startOperation = startOperation;
    }

    public CloudInstance getInstance() {
        return instance;
    }

    public boolean isStartOperation() {
        return startOperation;
    }
}
