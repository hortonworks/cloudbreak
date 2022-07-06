package com.sequenceiq.flow.core.helloworld.flowevents;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldFirstStepLongLastingTaskTriggerEvent extends HelloWorldSelectableEvent {

    @JsonCreator
    public HelloWorldFirstStepLongLastingTaskTriggerEvent(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

    @Override
    public String selector() {
        return getClass().getSimpleName();
    }

}
