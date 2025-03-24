package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CoreEnableSeLinuxEvent extends StackEvent {

    @JsonCreator
    public CoreEnableSeLinuxEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId) {
        super(selector, resourceId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreEnableSeLinuxEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("stackId=" + getResourceId())
                .toString();
    }
}
