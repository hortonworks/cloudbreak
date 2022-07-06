package com.sequenceiq.flow.core.helloworld.flowevents;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldSecondStepSuccessful extends HelloWorldSelectableEvent {

    @JsonCreator
    public HelloWorldSecondStepSuccessful(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

}
