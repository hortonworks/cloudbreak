package com.sequenceiq.flow.core.helloworld.flowevents;

import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_SOMETHING_WENT_WRONG;

public class HelloWorldFirstStepLongLastingTaskFailureResponse extends HelloWorldFailedEvent {

    public HelloWorldFirstStepLongLastingTaskFailureResponse(Long resourceId, Exception exception) {
        super(resourceId, exception);
    }

    @Override
    public String selector() {
        return HELLOWORLD_SOMETHING_WENT_WRONG.event();
    }

}
