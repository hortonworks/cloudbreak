package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RepairCrossRealmTrustV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepairCrossRealmTrustResponse {

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
        return "RepairCrossRealmTrustResponse{" +
                "flowIdentifier=" + flowIdentifier +
                ", operationStatus=" + operationStatus +
                '}';
    }
}
