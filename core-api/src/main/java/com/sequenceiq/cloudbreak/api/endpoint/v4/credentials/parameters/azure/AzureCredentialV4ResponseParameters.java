package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure;

import java.util.Map;

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
public class AzureCredentialV4ResponseParameters extends MappableBase implements JsonEntity {

    @ApiModelProperty
    private String subscriptionId;

    @ApiModelProperty
    private String tenantId;

    @ApiModelProperty
    private String accessKey;

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

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put("subscriptionId", subscriptionId);
        map.put("tenantId", tenantId);
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
        accessKey = getParameterOrNull(parameters, "accessKey");
        roleBased = new RoleBasedResponse();
        roleBased.parse(parameters);
    }

}
