package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StackCcmUpgradeV4Response {

    private FlowIdentifier flowIdentifier;

    public StackCcmUpgradeV4Response() {
    }

    public StackCcmUpgradeV4Response(FlowIdentifier flowIdentifier) {
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
        return "StackCcmUpgradeV4Response{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
