package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure;

import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.MappableBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureCredentialV4RequestParameters extends MappableBase implements JsonEntity {

    @NotNull
    @ApiModelProperty(required = true, example = "a8d4457d-310v-41p6-sc53-14g8d733e514")
    private String subscriptionId;

    @NotNull
    @ApiModelProperty(required = true, example = "b10u3481-2451-10ba-7sfd-9o2d1v60185d")
    private String tenantId;

    @Valid
    @ApiModelProperty
    private AppBasedRequest appBased;

    @Valid
    @ApiModelProperty
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
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put("subscriptionId", subscriptionId);
        map.put("tenantId", tenantId);
        if (appBased != null) {
            map.putAll(appBased.asMap());
        }
        if (roleBased != null) {
            map.putAll(roleBased.asMap());
        }
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        subscriptionId = getParameterOrNull(parameters, "subscriptionId");
        tenantId = getParameterOrNull(parameters, "tenantId");
        appBased = new AppBasedRequest();
        appBased.parse(parameters);
        roleBased = new RoleBasedRequest();
        roleBased.parse(parameters);
    }

}
