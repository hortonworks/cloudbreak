package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("GcpNetworkV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GcpNetworkParameters extends MappableBase {

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
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, "networkId", networkId);
        putIfValueNotNull(map, SUBNET_ID, subnetId);
        putIfValueNotNull(map, "sharedProjectId", sharedProjectId);
        putIfValueNotNull(map, "noFirewallRules", noFirewallRules);
        putIfValueNotNull(map, "noPublicIp", noPublicIp);
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        networkId = getParameterOrNull(parameters, "networkId");
        subnetId = getParameterOrNull(parameters, SUBNET_ID);
        sharedProjectId = getParameterOrNull(parameters, "sharedProjectId");
        noFirewallRules = getBoolean(parameters, "noFirewallRules");
        noPublicIp = getBoolean(parameters, "noPublicIp");
    }

    @Override
    public String toString() {
        return "GcpNetworkParameters{"
                + "networkId='" + networkId + '\''
                + ", subnetId='" + subnetId + '\''
                + ", sharedProjectId='" + sharedProjectId + '\''
                + ", noPublicIp=" + noPublicIp
                + ", noFirewallRules=" + noFirewallRules
                + '}';
    }
}
