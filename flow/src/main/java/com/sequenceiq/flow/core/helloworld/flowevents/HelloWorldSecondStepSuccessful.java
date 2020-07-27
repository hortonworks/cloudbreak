package com.sequenceiq.flow.core.helloworld.flowevents;

import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldSecondStepSuccessful extends HelloWorldSelectableEvent {

    public HelloWorldSecondStepSuccessful(Long resourceId) {
        super(resourceId);
    }

}
