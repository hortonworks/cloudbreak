package com.sequenceiq.flow.core.chain.finalize.flowevents;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.eventbus.Promise;

public class FlowChainFinalizeFailedPayload extends FlowChainFinalizePayload {

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
