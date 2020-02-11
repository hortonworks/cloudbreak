package com.sequenceiq.freeipa.flow.instance;

import java.util.List;

public class InstanceFailureEvent extends InstanceEvent {

    private final Exception exception;

    public InstanceFailureEvent(Long resourceId, Exception exception, List<String> instanceIds) {
        super(null, resourceId, instanceIds);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

}
