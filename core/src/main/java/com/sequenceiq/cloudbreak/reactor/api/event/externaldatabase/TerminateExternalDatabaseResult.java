package com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class TerminateExternalDatabaseResult extends ExternalDatabaseSelectableEvent {

    @JsonCreator
    public TerminateExternalDatabaseResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn) {
        super(resourceId, selector, resourceName, resourceCrn);
    }

    @Override
    public String selector() {
        return "TerminateExternalDatabaseResult";
    }
}
