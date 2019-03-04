package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

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
public class AzureNetworkV4Parameters extends MappableBase implements JsonEntity {

    @ApiModelProperty
    private Boolean noPublicIp;

    @ApiModelProperty
    private Boolean noFirewallRules;

    @ApiModelProperty
    private String resourceGroupName;

    @ApiModelProperty
    private String networkId;

    @ApiModelProperty
    private String subnetId;

    public Boolean getNoPublicIp() {
        return noPublicIp;
    }

    public void setNoPublicIp(Boolean noPublicIp) {
        this.noPublicIp = noPublicIp;
    }

    public Boolean getNoFirewallRules() {
        return noFirewallRules;
    }

    public void setNoFirewallRules(Boolean noFirewallRules) {
        this.noFirewallRules = noFirewallRules;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, "noPublicIp", noPublicIp);
        putIfValueNotNull(map, "noFirewallRules", noFirewallRules);
        putIfValueNotNull(map, "resourceGroupName", resourceGroupName);
        putIfValueNotNull(map, "networkId", networkId);
        putIfValueNotNull(map, "subnetId", subnetId);
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
        noPublicIp = getBoolean(parameters, "noPublicIp");
        noFirewallRules = getBoolean(parameters, "noFirewallRules");
        resourceGroupName = getParameterOrNull(parameters, "resourceGroupName");
        networkId = getParameterOrNull(parameters, "networkId");
        subnetId = getParameterOrNull(parameters, "subnetId");
    }
}
