package com.sequenceiq.periscope.api.model;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class DistroXAutoscaleClusterServerCertUpdateRequest implements Json {

    @ResourceCrn
    @Schema(description = ClusterJsonsProperties.STACK_CRN)
    private @NotNull String crn;

    @Schema(description = ClusterJsonsProperties.NEW_SERVER_CERT)
    private @NotNull String newServerCert;

    public DistroXAutoscaleClusterServerCertUpdateRequest() {
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getNewServerCert() {
        return newServerCert;
    }

    public void setNewServerCert(String newServerCert) {
        this.newServerCert = newServerCert;
    }
}
