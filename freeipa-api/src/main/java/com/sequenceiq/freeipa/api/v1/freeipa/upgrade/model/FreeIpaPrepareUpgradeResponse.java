package com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaPrepareUpgradeV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaPrepareUpgradeResponse {

    private FlowIdentifier flowIdentifier;

    private String operationId;

    public FreeIpaPrepareUpgradeResponse() {
    }

    public FreeIpaPrepareUpgradeResponse(FlowIdentifier flowIdentifier, String operationId) {
        this.flowIdentifier = flowIdentifier;
        this.operationId = operationId;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public String toString() {
        return "FreeIpaPrepareUpgradeResponse{" +
                "flowIdentifier=" + flowIdentifier +
                ", operationId='" + operationId + '\'' +
                '}';
    }
}
