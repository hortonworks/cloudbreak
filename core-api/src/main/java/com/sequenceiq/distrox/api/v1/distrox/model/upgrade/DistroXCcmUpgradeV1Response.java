package com.sequenceiq.distrox.api.v1.distrox.model.upgrade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistroXCcmUpgradeV1Response {

    private FlowIdentifier flowIdentifier;

    public DistroXCcmUpgradeV1Response() {
    }

    public DistroXCcmUpgradeV1Response(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "DistroXCcmUpgradeV1Response{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
