package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIPAVerticalScaleResponse implements JsonEntity {

    private FlowIdentifier flowIdentifier;

    private FreeIPAVerticalScaleRequest request;

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public FreeIPAVerticalScaleRequest getRequest() {
        return request;
    }

    public void setRequest(FreeIPAVerticalScaleRequest request) {
        this.request = request;
    }
}
