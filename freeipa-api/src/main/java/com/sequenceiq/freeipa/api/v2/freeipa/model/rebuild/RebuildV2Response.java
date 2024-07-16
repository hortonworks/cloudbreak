package com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RebuildV2Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RebuildV2Response extends RebuildV2Base {

    private FlowIdentifier flowIdentifier;

    private OperationStatus operationStatus;

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(OperationStatus operationStatus) {
        this.operationStatus = operationStatus;
    }

    @Override
    public String toString() {
        return "RebuildV2Response{" +
                "flowIdentifier=" + flowIdentifier +
                ", operationStatus=" + operationStatus +
                "} " + super.toString();
    }
}
