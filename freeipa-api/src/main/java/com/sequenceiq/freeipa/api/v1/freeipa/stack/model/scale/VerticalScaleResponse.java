package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerticalScaleResponse implements JsonEntity {

    private FlowIdentifier flowIdentifier;

    private VerticalScaleRequest request;

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public VerticalScaleRequest getRequest() {
        return request;
    }

    public void setRequest(VerticalScaleRequest request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return "VerticalScaleResponse{" +
                "flowIdentifier=" + flowIdentifier +
                ", request=" + request +
                '}';
    }
}
