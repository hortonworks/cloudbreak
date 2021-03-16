package com.sequenceiq.flow.core.chain.finalize.flowevents;

import reactor.rx.Promise;

public class FlowChainFinalizeFailedPayload extends FlowChainFinalizePayload {

    private Exception exception;

    public FlowChainFinalizeFailedPayload(String flowChainName, Long resourceId, Exception exception) {
        super(flowChainName, resourceId, new Promise<>());
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

}
