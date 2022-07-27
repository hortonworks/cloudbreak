package com.sequenceiq.flow.core.chain.init.flowevents;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import reactor.rx.Promise;

public class FlowChainInitFailedPayload extends FlowChainInitPayload {

    private final Exception exception;

    @JsonCreator
    public FlowChainInitFailedPayload(
            @JsonProperty("flowChainName") String flowChainName,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {

        super(flowChainName, resourceId, new Promise<>());
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
