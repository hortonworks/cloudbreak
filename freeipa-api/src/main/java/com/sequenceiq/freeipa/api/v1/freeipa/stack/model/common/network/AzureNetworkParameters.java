package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.NETWORK_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.NO_PUBLIC_IP;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.RESOURCE_GROUP_NAME;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AzureNetworkV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureNetworkParameters extends MappableBase {
    @Schema
    private Boolean noPublicIp;

    @Schema
    private String resourceGroupName;

    @Schema
    private String networkId;

    @Schema
    private String subnetId;

    public Boolean getNoPublicIp() {
        return noPublicIp;
    }

    public void setNoPublicIp(Boolean noPublicIp) {
        this.noPublicIp = noPublicIp;
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
        putIfValueNotNull(map, NO_PUBLIC_IP, noPublicIp);
        putIfValueNotNull(map, RESOURCE_GROUP_NAME, resourceGroupName);
        putIfValueNotNull(map, NETWORK_ID, networkId);
        putIfValueNotNull(map, SUBNET_ID, subnetId);
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        noPublicIp = getBoolean(parameters, NO_PUBLIC_IP);
        resourceGroupName = getParameterOrNull(parameters, RESOURCE_GROUP_NAME);
        networkId = getParameterOrNull(parameters, NETWORK_ID);
        subnetId = getParameterOrNull(parameters, SUBNET_ID);
    }

    @Override
    public String toString() {
        return "AzureNetworkParameters{"
                + "noPublicIp=" + noPublicIp
                + ", resourceGroupName='" + resourceGroupName + '\''
                + ", networkId='" + networkId + '\''
                + ", subnetId='" + subnetId + '\''
                + '}';
    }
}
