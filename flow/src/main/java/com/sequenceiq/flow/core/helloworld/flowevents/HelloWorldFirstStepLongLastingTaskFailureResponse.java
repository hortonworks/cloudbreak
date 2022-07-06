package com.sequenceiq.flow.core.helloworld.flowevents;

import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_SOMETHING_WENT_WRONG;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HelloWorldFirstStepLongLastingTaskFailureResponse extends HelloWorldFailedEvent {

    @JsonCreator
    public HelloWorldFirstStepLongLastingTaskFailureResponse(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {

        super(resourceId, exception);
    }

    @Override
    public String selector() {
        return HELLOWORLD_SOMETHING_WENT_WRONG.event();
    }

}
