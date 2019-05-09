package com.sequenceiq.flow.core.helloworld;

import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class HelloWorldFinishPayload extends BaseFlowEvent {
    public HelloWorldFinishPayload(String selector, Long resourceId) {
        super(selector, resourceId);
    }
}
