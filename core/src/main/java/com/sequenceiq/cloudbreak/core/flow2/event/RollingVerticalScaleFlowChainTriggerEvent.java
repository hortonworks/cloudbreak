package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RollingVerticalScaleFlowChainTriggerEvent extends StackEvent {

    private final StackVerticalScaleV4Request request;

    @JsonCreator
    public RollingVerticalScaleFlowChainTriggerEvent(
            @JsonProperty("selector") String event,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("request") StackVerticalScaleV4Request request) {
        super(event, resourceId);
        this.request = request;
    }

    public StackVerticalScaleV4Request getRequest() {
        return request;
    }
}
