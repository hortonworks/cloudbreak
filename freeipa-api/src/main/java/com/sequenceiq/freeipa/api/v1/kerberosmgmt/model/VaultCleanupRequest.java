package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VaultCleanupV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VaultCleanupRequest {

    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    @NotNull
    private String environmentCrn;

    @Schema(description = ModelDescriptions.CLUSTER_CRN)
    private String clusterCrn;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public void setClusterCrn(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    @Override
    public String toString() {
        return "VaultCleanupRequest{"
                + "environmentCrn='" + environmentCrn + '\''
                + ", clusterCrn='" + clusterCrn + '\''
                + '}';
    }
}
