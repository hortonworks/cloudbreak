package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class ExternalDatabaseCommenceStopEvent extends ExternalDatabaseSelectableEvent {

    @JsonCreator
    public ExternalDatabaseCommenceStopEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn) {
        super(resourceId, selector, resourceName, resourceCrn);
    }
}
