package com.sequenceiq.environment.api.v1.environment.model.request.gcp;

import java.io.Serializable;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "GcpEnvironmentV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpEnvironmentParameters implements Serializable {

    @Valid
    @Schema(description = EnvironmentModelDescription.GCP_RESOURCE_ENCRYPTION_PARAMETERS)
    private GcpResourceEncryptionParameters gcpResourceEncryptionParameters;

    public GcpEnvironmentParameters() {
    }

    private GcpEnvironmentParameters(GcpEnvironmentParameters.Builder builder) {
        gcpResourceEncryptionParameters = builder.gcpResourceEncryptionParameters;
    }

    public GcpResourceEncryptionParameters getGcpResourceEncryptionParameters() {
        return gcpResourceEncryptionParameters;
    }

    public void setGcpResourceEncryptionParameters(GcpResourceEncryptionParameters gcpResourceEncryptionParameters) {
        this.gcpResourceEncryptionParameters = gcpResourceEncryptionParameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "GcpResourceEncryptionParameters{" +
                ", gcpResourceEncryptionParameters=" + gcpResourceEncryptionParameters +
                '}';
    }

    public static final class Builder {
        private GcpResourceEncryptionParameters gcpResourceEncryptionParameters;

        private Builder() {
        }

        public Builder withResourceEncryptionParameters(GcpResourceEncryptionParameters gcpResourceEncryptionParameters) {
            this.gcpResourceEncryptionParameters = gcpResourceEncryptionParameters;
            return this;
        }

        public GcpEnvironmentParameters build() {
            return new GcpEnvironmentParameters(this);
        }

    }

}
