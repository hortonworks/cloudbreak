package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.FreeIPAVerticalScaleRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIPAVerticalScalingTriggerEvent extends StackEvent {

    private FreeIPAVerticalScaleRequest request;

    @JsonCreator
    public FreeIPAVerticalScalingTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("request") FreeIPAVerticalScaleRequest request) {
        super(selector, stackId);
        this.request = request;
    }

    public FreeIPAVerticalScaleRequest getRequest() {
        return request;
    }

    public void setRequest(FreeIPAVerticalScaleRequest request) {
        this.request = request;
    }
}
