package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AzureNetworkV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureNetworkParameters extends MappableBase {
    @ApiModelProperty
    private Boolean noPublicIp;

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
        resourceGroupName = getParameterOrNull(parameters, "resourceGroupName");
        networkId = getParameterOrNull(parameters, "networkId");
        subnetId = getParameterOrNull(parameters, "subnetId");
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
