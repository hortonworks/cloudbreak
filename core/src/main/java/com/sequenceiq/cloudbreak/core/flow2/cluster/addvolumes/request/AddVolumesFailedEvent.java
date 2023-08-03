package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.request;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FAILURE_EVENT;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class AddVolumesFailedEvent extends BaseFailedFlowEvent implements Selectable {

    @JsonCreator
    public AddVolumesFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("exception") Exception exception) {
        super(FAILURE_EVENT.event(), resourceId, null, resourceName, resourceCrn, exception);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AddVolumesFailedEvent.class.getSimpleName() + "[", "]")
                .toString();
    }
}