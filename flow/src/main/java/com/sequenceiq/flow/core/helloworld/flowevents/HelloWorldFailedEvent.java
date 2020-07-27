package com.sequenceiq.flow.core.helloworld.flowevents;

import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldFailedEvent extends HelloWorldSelectableEvent {

    private Exception exception;

    public HelloWorldFailedEvent(Long resourceId, Exception exception) {
        super(resourceId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

}
