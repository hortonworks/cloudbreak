package com.sequenceiq.freeipa.api.v1.freeipa.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaMultiAzMigrationV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaMultiAzMigrationV1Response {

    private FlowIdentifier flowIdentifier;

    private String operationId;

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
        return "FreeIpaMultiAzMigrationV1Response{" +
                "flowIdentifier=" + flowIdentifier +
                ", operationId='" + operationId + '\'' +
                '}';
    }
}
