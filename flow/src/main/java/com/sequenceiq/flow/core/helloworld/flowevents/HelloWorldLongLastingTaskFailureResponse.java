package com.sequenceiq.flow.core.helloworld.flowevents;

import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldLongLastingTaskFailureResponse extends HelloWorldSelectableEvent {
    private Exception exception;

    public HelloWorldLongLastingTaskFailureResponse(Long resourceId, Exception exception) {
        super(resourceId);
        this.exception = exception;
    }

    public String selector() {
        return failureSelector(getClass());
    }

    public Exception getException() {
        return exception;
    }
}
