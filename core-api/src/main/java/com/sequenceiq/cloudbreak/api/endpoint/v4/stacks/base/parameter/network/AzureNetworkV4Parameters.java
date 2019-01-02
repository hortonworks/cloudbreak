package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class AzureNetworkV4Parameters implements JsonEntity, Mappable {

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
        Map<String, Object> ret = new HashMap<>();
        ret.put("noPublicIp", noPublicIp);
        ret.put("noFirewallRules", noFirewallRules);
        ret.put("resourceGroupName", resourceGroupName);
        ret.put("networkId", networkId);
        ret.put("subnetId", subnetId);
        return ret;
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
