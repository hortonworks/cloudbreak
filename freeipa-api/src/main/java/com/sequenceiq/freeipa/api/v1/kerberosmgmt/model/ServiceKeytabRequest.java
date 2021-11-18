package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ServiceKeytabV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceKeytabRequest {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    @NotNull
    private String environmentCrn;

    @ApiModelProperty(value = KeytabModelDescription.SERVICE_NAME, required = true)
    @NotEmpty
    private String serviceName;

    @ApiModelProperty(value = KeytabModelDescription.SERVICE_HOST, required = true)
    @NotEmpty
    private String serverHostName;

    @ApiModelProperty(value = KeytabModelDescription.SERVICE_HOST_ALIAS)
    private String serverHostNameAlias;

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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public String getServerHostNameAlias() {
        return serverHostNameAlias;
    }

    public void setServerHostNameAlias(String serverHostNameAlias) {
        this.serverHostNameAlias = serverHostNameAlias;
    }

    @Override
    public String toString() {
        return "ServiceKeytabRequest{"
                + "environmentCrn='" + environmentCrn + '\''
                + ", serviceName='" + serviceName + '\''
                + ", serverHostName='" + serverHostName + '\''
                + ", serverHostNameAlias='" + serverHostNameAlias + '\''
                + ", clusterCrn='" + clusterCrn + '\''
                + ", doNotRecreateKeytab=" + doNotRecreateKeytab
                + ", roleRequest=" + roleRequest
                + '}';
    }
}
