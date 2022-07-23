package com.sequenceiq.flow.core.chain.finalize.flowevents;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import reactor.rx.Promise;

public class FlowChainFinalizeFailedPayload extends FlowChainFinalizePayload {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    @JsonCreator
    public FlowChainFinalizeFailedPayload(
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
