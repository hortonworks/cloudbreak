package com.sequenceiq.environment.api.v1.environment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AzureExternalizedComputeParams")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureExternalizedComputeParams {

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_OUTBOUND_TYPE)
    private String outboundType;

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getOutboundType() {
        return outboundType;
    }

    public void setOutboundType(String outboundType) {
        this.outboundType = outboundType;
    }

    @Override
    public String toString() {
        return "AzureExternalizedComputeParams{" +
                "outboundType='" + outboundType + '\'' +
                '}';
    }

    public static class Builder {

        private String outboundType;

        public Builder withOutboundType(String outboundType) {
            this.outboundType = outboundType;
            return this;
        }

        public AzureExternalizedComputeParams build() {
            AzureExternalizedComputeParams azureExternalizedComputeParams = new AzureExternalizedComputeParams();
            azureExternalizedComputeParams.setOutboundType(outboundType);
            return azureExternalizedComputeParams;
        }
    }
}
