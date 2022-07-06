package com.sequenceiq.flow.core.helloworld.flowevents;

import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_FIRST_STEP_FINISHED_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldFirstStepLongLastingTaskSuccessResponse extends HelloWorldSelectableEvent {

    @JsonCreator
    public HelloWorldFirstStepLongLastingTaskSuccessResponse(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

    @Override
    public String selector() {
        return HELLOWORLD_FIRST_STEP_FINISHED_EVENT.event();
    }

}
