package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseFlowIdentifierResponse implements FlowIdentifierResponse, JsonEntity {

    @NotNull
    @Schema(description = FreeIpaModelDescriptions.FLOWIDENTIFIER, requiredMode = Schema.RequiredMode.REQUIRED)
    private FlowIdentifier flowIdentifier;

    @Override
    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "BaseFlowIdentifierResponse{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }
}
