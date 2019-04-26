package com.sequenceiq.cloudbreak.core.flow2.helloworld;

import com.sequenceiq.cloudbreak.reactor.api.event.BaseFlowEvent;

public class HelloWorldFinishPayload extends BaseFlowEvent {
    public HelloWorldFinishPayload(String selector, Long resourceId) {
        super(selector, resourceId);
    }
}
