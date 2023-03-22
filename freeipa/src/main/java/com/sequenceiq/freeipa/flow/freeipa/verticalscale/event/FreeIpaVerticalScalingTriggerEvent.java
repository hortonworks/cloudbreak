package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaVerticalScalingTriggerEvent extends StackEvent {

    private VerticalScaleRequest request;

    private String operationId;

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

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public FreeIpaVerticalScalingTriggerEvent withOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    @Override
    public String toString() {
        return "FreeIpaVerticalScalingTriggerEvent{" +
                "request=" + request +
                ", operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
