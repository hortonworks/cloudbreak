package com.sequenceiq.environment.api.v1.environment.model;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.dataservices.DataServicesModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.BaseDataServicesV1Parameters;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureDataServicesV1Parameters extends BaseDataServicesV1Parameters {

    @Schema(description = DataServicesModelDescription.SHARED_MANAGED_IDENTITY)
    @NotNull
    private String sharedManagedIdentity;

    public String getSharedManagedIdentity() {
        return sharedManagedIdentity;
    }

    public void setSharedManagedIdentity(String sharedManagedIdentity) {
        this.sharedManagedIdentity = sharedManagedIdentity;
    }

    @Override
    public String toString() {
        return "AzureDataServicesV1Parameters{" +
                "sharedManagedIdentity='" + sharedManagedIdentity + '\'' +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AzureDataServicesV1Parameters that)) {
            return false;
        }
        return Objects.equals(sharedManagedIdentity, that.sharedManagedIdentity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sharedManagedIdentity);
    }
}
