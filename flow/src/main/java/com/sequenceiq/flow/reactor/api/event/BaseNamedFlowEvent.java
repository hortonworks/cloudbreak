package com.sequenceiq.flow.reactor.api.event;

import reactor.rx.Promise;

public class BaseNamedFlowEvent extends BaseFlowEvent {

    private final String resourceName;

    public BaseNamedFlowEvent(String selector, Long resourceId, String resourceName) {
        super(selector, resourceId);
        this.resourceName = resourceName;
    }

    public BaseNamedFlowEvent(String selector, Long resourceId, Promise<Boolean> accepted, String resourceName) {
        super(selector, resourceId, accepted);
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }
}
