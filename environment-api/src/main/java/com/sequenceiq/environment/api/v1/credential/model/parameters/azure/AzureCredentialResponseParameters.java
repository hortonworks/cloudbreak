package com.sequenceiq.environment.api.v1.credential.model.parameters.azure;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AzureCredentialV1ResponseParameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureCredentialResponseParameters implements Serializable {

    @ApiModelProperty
    private String subscriptionId;

    @ApiModelProperty
    private String tenantId;

    @ApiModelProperty
    private String accessKey;

    @ApiModelProperty
    private String authenticationType;

    @ApiModelProperty
    private List<AzureCertificateResponse> certificates;

    @ApiModelProperty
    private RoleBasedResponse roleBased;

    public RoleBasedResponse getRoleBased() {
        return roleBased;
    }

    public void setRoleBased(RoleBasedResponse roleBased) {
        this.roleBased = roleBased;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    public List<AzureCertificateResponse> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<AzureCertificateResponse> certificates) {
        this.certificates = certificates;
    }

    @Override
    public String toString() {
        return "AzureCredentialResponseParameters{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", authenticationType='" + authenticationType + '\'' +
                ", certificates=" + certificates +
                ", roleBased=" + roleBased +
                '}';
    }
}
