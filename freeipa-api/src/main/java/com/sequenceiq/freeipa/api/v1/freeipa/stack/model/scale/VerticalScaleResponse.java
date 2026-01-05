package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.BaseFlowIdentifierResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerticalScaleResponse extends BaseFlowIdentifierResponse {

    private VerticalScaleRequest request;

    public VerticalScaleRequest getRequest() {
        return request;
    }

    public void setRequest(VerticalScaleRequest request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return "VerticalScaleResponse{" +
                super.toString() +
                ", request=" + request +
                '}';
    }
}
