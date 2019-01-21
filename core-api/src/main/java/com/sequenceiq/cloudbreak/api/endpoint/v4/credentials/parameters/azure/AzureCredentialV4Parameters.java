package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.CredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AzureCredentialV4Parameters implements CredentialV4Parameters {

    @NotNull
    @ApiModelProperty(required = true, example = "a8d4457d-310v-41p6-sc53-14g8d733e514")
    private String subscriptionId;

    @NotNull
    @ApiModelProperty(required = true, example = "b10u3481-2451-10ba-7sfd-9o2d1v60185d")
    private String tenantId;

    @Valid
    @ApiModelProperty
    private AppBased appBased;

    @Valid
    @ApiModelProperty
    private RoleBased roleBased;

    public RoleBased getRoleBased() {
        return roleBased;
    }

    public void setRoleBased(RoleBased roleBased) {
        this.roleBased = roleBased;
    }

    public AppBased getAppBased() {
        return appBased;
    }

    public void setAppBased(AppBased appBased) {
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
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
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

}
