package com.sequenceiq.environment.api.v1.credential.model.parameters.azure;

import java.io.Serializable;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AzureCredentialV1RequestParameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureCredentialRequestParameters implements Serializable {

    @Schema(example = "a8d4457d-310v-41p6-sc53-14g8d733e514")
    private String subscriptionId;

    @Schema(example = "b10u3481-2451-10ba-7sfd-9o2d1v60185d")
    private String tenantId;

    @Valid
    @Schema
    private AppBasedRequest appBased;

    @Valid
    @Schema
    private RoleBasedRequest roleBased;

    public RoleBasedRequest getRoleBased() {
        return roleBased;
    }

    public void setRoleBased(RoleBasedRequest roleBased) {
        this.roleBased = roleBased;
    }

    public AppBasedRequest getAppBased() {
        return appBased;
    }

    public void setAppBased(AppBasedRequest appBased) {
        this.appBased = appBased;
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

    @Override
    public String toString() {
        return "AzureCredentialRequestParameters{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", appBased=" + appBased +
                ", roleBased=" + roleBased +
                '}';
    }
}
