package com.sequenceiq.flow.core.chain.init.flowevents;

import reactor.rx.Promise;

public class FlowChainInitFailedPayload extends FlowChainInitPayload {

    private Exception exception;

    public FlowChainInitFailedPayload(String flowChainName, Long resourceId, Exception exception) {
        super(flowChainName, resourceId, new Promise<>());
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
