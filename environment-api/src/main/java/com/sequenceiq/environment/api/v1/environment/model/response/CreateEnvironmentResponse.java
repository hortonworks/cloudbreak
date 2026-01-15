package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "CreateEnvironmentResponse")
public class CreateEnvironmentResponse extends DetailedEnvironmentResponse {

    @Schema(description = FreeIpaModelDescriptions.FLOWIDENTIFIER, requiredMode = Schema.RequiredMode.REQUIRED)
    private FlowIdentifier flowIdentifier;

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public static CreateEnvironmentResponse.Builder builder() {
        return new CreateEnvironmentResponse.Builder();
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "CreateEnvironmentResponse{" +
                "flowIdentifier=" + flowIdentifier +
                '}';
    }

    public static class Builder extends DetailedEnvironmentResponse.Builder<CreateEnvironmentResponse, Builder> {
        private FlowIdentifier flowIdentifier;

        private Builder() {
        }

        public Builder withFlowIdentifier(FlowIdentifier flowIdentifier) {
            this.flowIdentifier = flowIdentifier;
            return this;
        }

        @Override
        public CreateEnvironmentResponse build() {
            CreateEnvironmentResponse response = super.build(new CreateEnvironmentResponse());
            response.setFlowIdentifier(flowIdentifier);
            return response;
        }
    }
}
