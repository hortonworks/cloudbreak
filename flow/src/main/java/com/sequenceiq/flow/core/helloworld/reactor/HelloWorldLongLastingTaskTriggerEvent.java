package com.sequenceiq.flow.core.helloworld.reactor;

import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldLongLastingTaskTriggerEvent extends HelloWorldSelectableEvent {
    public HelloWorldLongLastingTaskTriggerEvent(Long resourceId) {
        super(resourceId);
    }

    public HelloWorldLongLastingTaskTriggerEvent(Long resourceId, String selector) {
        super(resourceId, selector);
    }
}
