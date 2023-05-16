package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class DiskResizeFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final Exception errorDetails;

    @JsonCreator
    public DiskResizeFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("errorDetails") Exception errorDetails) {
        super(selector, resourceId, null, resourceName, resourceCrn, errorDetails);
        this.errorDetails = errorDetails;
    }

    public Exception getErrorDetails() {
        return errorDetails;
    }

    public String toString() {
        return new StringJoiner(", ", DiskResizeFailedEvent.class.getSimpleName() + "[", "]")
                .add("errorDetails=" + errorDetails)
                .toString();
    }
}
