package com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class TerminateExternalDatabaseRequest extends ExternalDatabaseSelectableEvent {

    private final boolean forced;

    @JsonCreator
    public TerminateExternalDatabaseRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("forced") boolean forced) {
        super(resourceId, selector, resourceName, resourceCrn);
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }
}
