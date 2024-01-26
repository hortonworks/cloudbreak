package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExternalizedComputeRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalizedComputeCreateRequest implements Serializable {

    @NotNull
    @Schema(description = EnvironmentModelDescription.CREATE_EXTERNALIZED_COMPUTE_CLUSTER)
    private boolean create;

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeRequest{" +
                "create=" + create +
                '}';
    }
}
