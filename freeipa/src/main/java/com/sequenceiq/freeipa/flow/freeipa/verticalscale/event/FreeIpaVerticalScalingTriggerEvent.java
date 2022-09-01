package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaVerticalScalingTriggerEvent extends StackEvent {

    private VerticalScaleRequest request;

    @JsonCreator
    public FreeIpaVerticalScalingTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("request") VerticalScaleRequest request) {
        super(selector, stackId);
        this.request = request;
    }

    public VerticalScaleRequest getRequest() {
        return request;
    }

    public void setRequest(VerticalScaleRequest request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return super.toString() + "FreeIpaVerticalScalingTriggerEvent{" +
                "request=" + request +
                '}';
    }
}
