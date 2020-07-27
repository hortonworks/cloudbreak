package com.sequenceiq.flow.core.helloworld.flowevents;

import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldFirstStepLongLastingTaskTriggerEvent extends HelloWorldSelectableEvent {

    public HelloWorldFirstStepLongLastingTaskTriggerEvent(Long resourceId) {
        super(resourceId);
    }

    @Override
    public String selector() {
        return getClass().getSimpleName();
    }

}
