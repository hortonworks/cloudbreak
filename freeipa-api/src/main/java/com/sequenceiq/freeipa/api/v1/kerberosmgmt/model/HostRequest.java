package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("HostV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostRequest {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    @NotNull
    private String environmentCrn;

    @ApiModelProperty(value = KeytabModelDescription.SERVICE_HOST, required = true)
    @NotNull
    private String serverHostName;

    @ApiModelProperty(value = ModelDescriptions.CLUSTER_CRN)
    private String clusterCrn;

    @ApiModelProperty(value = KeytabModelDescription.ROLE_NAME)
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
