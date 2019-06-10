package com.sequenceiq.flow.core.helloworld.flowevents;

import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldLongLastingTaskSuccessResponse extends HelloWorldSelectableEvent {
    public HelloWorldLongLastingTaskSuccessResponse(Long resourceId) {
        super(resourceId);
    }
}
