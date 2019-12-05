package com.sequenceiq.flow.reactor.api.event;

import reactor.rx.Promise;

public class BaseFailedFlowEvent extends BaseNamedFlowEvent {

    private final Exception exception;

    public BaseFailedFlowEvent(String selector, Long resourceId, String resourceName, String resourceCrn, Exception exception) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.exception = exception;
    }

    public BaseFailedFlowEvent(String selector, Long resourceId, Promise<Boolean> accepted,
            String resourceName, String resourceCrn, Exception exception) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
