package com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class TerminateExternalDatabaseFailed extends ExternalDatabaseSelectableEvent {

    private final Exception exception;

    @JsonCreator
    public TerminateExternalDatabaseFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("exception") Exception exception) {
        super(resourceId, selector, resourceName, resourceCrn);
        this.exception = exception;
    }

    public String selector() {
        return "TerminateExternalDatabaseFailed";
    }

    public Exception getException() {
        return exception;
    }

}
