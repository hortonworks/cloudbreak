package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FAILURE_EVENT;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class AddVolumesFailedEvent extends StackFailureEvent {

    @JsonCreator
    public AddVolumesFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(FAILURE_EVENT.event(), resourceId, exception);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AddVolumesFailedEvent.class.getSimpleName() + "[", "]")
                .toString();
    }
}