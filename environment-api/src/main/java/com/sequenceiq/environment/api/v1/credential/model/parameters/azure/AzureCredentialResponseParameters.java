package com.sequenceiq.environment.api.v1.credential.model.parameters.azure;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.api.credential.AppAuthenticationType;

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
    private AppAuthenticationType authenticationType;

    @ApiModelProperty
    private AzureCredentialCertificateResponse certificate;

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

    public AppAuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(AppAuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
    }

    public AzureCredentialCertificateResponse getCertificate() {
        return certificate;
    }

    public void setCertificate(AzureCredentialCertificateResponse certificate) {
        this.certificate = certificate;
    }

    @Override
    public String toString() {
        return "AzureCredentialResponseParameters{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", authenticationType='" + authenticationType + '\'' +
                ", certificate=" + certificate +
                ", roleBased=" + roleBased +
                '}';
    }
}
