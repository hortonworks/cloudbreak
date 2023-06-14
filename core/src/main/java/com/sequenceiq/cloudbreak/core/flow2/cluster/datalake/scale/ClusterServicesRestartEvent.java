package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class ClusterServicesRestartEvent extends BaseNamedFlowEvent {

    private Exception exception;

    @JsonCreator
    public ClusterServicesRestartEvent(@JsonProperty("selector") String selector, @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName, @JsonProperty("resourceCrn") String resourceCrn) {
        super(selector, resourceId, resourceName, resourceCrn);
    }

    public ClusterServicesRestartEvent(String selector, Long resourceId, String resourceName, String resourceCrn, Exception exception) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
