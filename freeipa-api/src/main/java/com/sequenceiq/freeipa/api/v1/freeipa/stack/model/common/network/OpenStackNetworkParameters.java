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

@ApiModel("OpenStackNetworkV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Deprecated
public class OpenStackNetworkParameters extends MappableBase {

    @ApiModelProperty
    private String networkId;

    @ApiModelProperty
    private String routerId;

    @ApiModelProperty
    private String subnetId;

    @ApiModelProperty
    private String publicNetId;

    @ApiModelProperty
    private String networkingOption;

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getPublicNetId() {
        return publicNetId;
    }

    public void setPublicNetId(String publicNetId) {
        this.publicNetId = publicNetId;
    }

    public String getNetworkingOption() {
        return networkingOption;
    }

    public void setNetworkingOption(String networkingOption) {
        this.networkingOption = networkingOption;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, "networkId", networkId);
        putIfValueNotNull(map, "routerId", routerId);
        putIfValueNotNull(map, "subnetId", subnetId);
        putIfValueNotNull(map, "publicNetId", publicNetId);
        putIfValueNotNull(map, "networkingOption", networkingOption);
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        throw new IllegalStateException("OPENSTACK is deprecated");
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        networkId = getParameterOrNull(parameters, "networkId");
        routerId = getParameterOrNull(parameters, "routerId");
        subnetId = getParameterOrNull(parameters, "subnetId");
        publicNetId = getParameterOrNull(parameters, "publicNetId");
        networkingOption = getParameterOrNull(parameters, "networkingOption");
    }

    @Override
    public String toString() {
        return "OpenStackNetworkParameters{"
                + "networkId='" + networkId + '\''
                + ", routerId='" + routerId + '\''
                + ", subnetId='" + subnetId + '\''
                + ", publicNetId='" + publicNetId + '\''
                + ", networkingOption='" + networkingOption + '\''
                + '}';
    }
}
