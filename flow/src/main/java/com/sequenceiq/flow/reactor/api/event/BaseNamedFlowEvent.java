package com.sequenceiq.flow.reactor.api.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;

import reactor.rx.Promise;

public class BaseNamedFlowEvent extends BaseFlowEvent {

    private final String resourceName;

    public BaseNamedFlowEvent(String selector, Long resourceId, String resourceName, String resourceCrn) {
        super(selector, resourceId, resourceCrn);
        this.resourceName = resourceName;
    }

    public BaseNamedFlowEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName, String resourceCrn) {
        super(selector, resourceId, resourceCrn, accepted);
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }
}
