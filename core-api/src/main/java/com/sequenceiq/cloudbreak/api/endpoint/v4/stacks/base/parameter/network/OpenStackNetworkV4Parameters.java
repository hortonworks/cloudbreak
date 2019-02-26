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
public class OpenStackNetworkV4Parameters extends MappableBase implements JsonEntity {

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
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        networkId = getParameterOrNull(parameters, "networkId");
        routerId = getParameterOrNull(parameters, "routerId");
        subnetId = getParameterOrNull(parameters, "subnetId");
        publicNetId = getParameterOrNull(parameters, "publicNetId");
        networkingOption = getParameterOrNull(parameters, "networkingOption");
    }
}
