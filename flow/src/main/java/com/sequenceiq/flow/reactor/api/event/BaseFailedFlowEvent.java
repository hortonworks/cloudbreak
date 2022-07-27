package com.sequenceiq.flow.reactor.api.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;

import reactor.rx.Promise;

public class BaseFailedFlowEvent extends BaseNamedFlowEvent {

    private final Exception exception;

    public BaseFailedFlowEvent(String selector, Long resourceId, String resourceName, String resourceCrn, Exception exception) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.exception = exception;
    }

    @JsonCreator
    public BaseFailedFlowEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("exception") Exception exception) {

        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
