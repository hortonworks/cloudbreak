package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class OpenStackNetworkV4Parameters implements JsonEntity, Mappable {

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
        Map<String, Object> map = new HashMap<>();
        map.put("networkId", networkId);
        map.put("routerId", routerId);
        map.put("subnetId", subnetId);
        map.put("publicNetId", publicNetId);
        map.put("networkingOption", networkingOption);
        return map;
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
