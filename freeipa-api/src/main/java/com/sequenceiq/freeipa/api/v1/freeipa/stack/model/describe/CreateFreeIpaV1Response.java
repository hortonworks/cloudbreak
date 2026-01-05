package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.FlowIdentifierResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateFreeIpaV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateFreeIpaV1Response extends DescribeFreeIpaResponse implements FlowIdentifierResponse {

    @Schema(description = FreeIpaModelDescriptions.FLOWIDENTIFIER)
    private FlowIdentifier flowIdentifier;

    @Override
    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }
}
