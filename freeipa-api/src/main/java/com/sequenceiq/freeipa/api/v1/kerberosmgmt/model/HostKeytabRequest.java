package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("HostKeytabV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostKeytabRequest {

    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    @NotNull
    private String environmentCrn;

    @NotNull
    private String serverHostName;

    @ApiModelProperty(value = ModelDescriptions.CLUSTER_CRN)
    private String clusterCrn;

    @ApiModelProperty(value = KeytabModelDescription.DO_NOT_RECREATE_KEYTAB)
    private Boolean doNotRecreateKeytab = Boolean.FALSE;

    @ApiModelProperty(value = KeytabModelDescription.ROLE)
    private RoleRequest roleRequest;

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

    public Boolean getDoNotRecreateKeytab() {
        return doNotRecreateKeytab;
    }

    public void setDoNotRecreateKeytab(Boolean doNotRecreateKeytab) {
        this.doNotRecreateKeytab = doNotRecreateKeytab;
    }

    public RoleRequest getRoleRequest() {
        return roleRequest;
    }

    public void setRoleRequest(RoleRequest roleRequest) {
        this.roleRequest = roleRequest;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public void setClusterCrn(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    @Override
    public String toString() {
        return "HostKeytabRequest{"
                + "environmentCrn='" + environmentCrn + '\''
                + ", serverHostName='" + serverHostName + '\''
                + ", clusterCrn='" + clusterCrn + '\''
                + ", doNotRecreateKeytab=" + doNotRecreateKeytab
                + ", roleRequest=" + roleRequest
                + '}';
    }
}
