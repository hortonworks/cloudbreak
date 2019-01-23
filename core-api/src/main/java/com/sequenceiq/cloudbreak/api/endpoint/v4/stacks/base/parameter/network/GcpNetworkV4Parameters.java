package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class GcpNetworkV4Parameters implements JsonEntity, Mappable {

    @ApiModelProperty
    private String networkId;

    @ApiModelProperty
    private String subnetId;

    @ApiModelProperty
    private String sharedProjectId;

    @ApiModelProperty
    private Boolean noPublicIp;

    @ApiModelProperty
    private Boolean noFirewallRules;

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

    public String getSharedProjectId() {
        return sharedProjectId;
    }

    public void setSharedProjectId(String sharedProjectId) {
        this.sharedProjectId = sharedProjectId;
    }

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

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("networkId", networkId);
        map.put("subnetId", subnetId);
        map.put("sharedProjectId", sharedProjectId);
        map.put("noFirewallRules", noFirewallRules);
        map.put("noPublicIp", noPublicIp);
        return map;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        networkId = getParameterOrNull(parameters,"networkId");
        subnetId = getParameterOrNull(parameters,"subnetId");
        sharedProjectId = getParameterOrNull(parameters,"sharedProjectId");
        noFirewallRules = getBoolean(parameters,"noFirewallRules");
        noPublicIp = getBoolean(parameters,"noPublicIp");
    }
}
