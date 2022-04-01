package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDatabaseRestoreResponse {

    @ApiModelProperty(ModelDescriptions.OPERATION_ID)
    private String operationId;

    @ApiModelProperty(ModelDescriptions.FLOW_IDENTIFIER)
    private FlowIdentifier flowIdentifier;

    public SdxDatabaseRestoreResponse() {
    }

    public SdxDatabaseRestoreResponse(String operationId, FlowIdentifier flowIdentifier) {
        this.operationId = operationId;
        this.flowIdentifier = flowIdentifier;
    }

    public String getOperationId() {
        return operationId;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public String toString() {
        return "SdxDatabaseRestoreResponse{" +
                "FlowIdentifier= " + flowIdentifier.toString() +
                "OperationId=" + operationId +
                '}';
    }
}
