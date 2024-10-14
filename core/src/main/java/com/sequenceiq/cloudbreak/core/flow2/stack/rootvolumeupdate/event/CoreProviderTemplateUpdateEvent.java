package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CoreProviderTemplateUpdateEvent extends StackEvent {

    @JsonCreator
    public CoreProviderTemplateUpdateEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreProviderTemplateUpdateEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("stackId=" + getResourceId())
                .toString();
    }
}
