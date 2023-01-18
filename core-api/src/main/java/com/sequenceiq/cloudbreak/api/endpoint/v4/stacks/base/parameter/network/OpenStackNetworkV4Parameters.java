package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Deprecated
public class OpenStackNetworkV4Parameters extends MappableBase implements JsonEntity {

    @Schema
    private String networkId;

    @Schema
    private String routerId;

    @Schema
    private String subnetId;

    @Schema
    private String publicNetId;

    @Schema
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
        putIfValueNotNull(map, SUBNET_ID, subnetId);
        putIfValueNotNull(map, "publicNetId", publicNetId);
        putIfValueNotNull(map, "networkingOption", networkingOption);
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        throw new IllegalStateException("OPENSTACK is deprecated");
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        networkId = getParameterOrNull(parameters, "networkId");
        routerId = getParameterOrNull(parameters, "routerId");
        subnetId = getParameterOrNull(parameters, SUBNET_ID);
        publicNetId = getParameterOrNull(parameters, "publicNetId");
        networkingOption = getParameterOrNull(parameters, "networkingOption");
    }
}
