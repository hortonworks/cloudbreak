package com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class UserOperationExternalDatabaseFailed extends ExternalDatabaseSelectableEvent {

    private final Exception exception;

    @JsonCreator
    public UserOperationExternalDatabaseFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("exception") Exception exception) {
        super(resourceId, selector, resourceName, resourceCrn);
        this.exception = exception;
    }

    public String selector() {
        return selector(getClass());
    }

    public Exception getException() {
        return exception;
    }

}
