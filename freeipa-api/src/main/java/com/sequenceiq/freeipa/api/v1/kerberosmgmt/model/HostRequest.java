package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HostV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostRequest {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private String environmentCrn;

    @Schema(description = KeytabModelDescription.SERVICE_HOST, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private String serverHostName;

    @Schema(description = ModelDescriptions.CLUSTER_CRN)
    private String clusterCrn;

    @Schema(description = KeytabModelDescription.ROLE_NAME)
    private String roleName;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getServerHostName() {
        return serverHostName;
    }

    public void setServerHostName(String serverHostName) {
        this.serverHostName = serverHostName;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public void setClusterCrn(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public String toString() {
        return "HostRequest{"
                + "environmentCrn='" + environmentCrn + '\''
                + ", serverHostName='" + serverHostName + '\''
                + ", clusterCrn='" + clusterCrn + '\''
                + ", roleName='" + roleName + '\''
                + '}';
    }
}
