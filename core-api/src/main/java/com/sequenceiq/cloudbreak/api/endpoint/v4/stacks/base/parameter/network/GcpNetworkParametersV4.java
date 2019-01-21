package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class GcpNetworkParametersV4 implements JsonEntity, Mappable {

    @ApiModelProperty
    private String networkId;

    @ApiModelProperty
    private String subnetId;

    @ApiModelProperty
    private String sharedProjectId;

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

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("networkId", networkId);
        map.put("subnetId", subnetId);
        map.put("sharedProjectId", sharedProjectId);
        return map;
    }

    @Override
    public <T> T toClass(Map<String, Object> parameters) {
        networkId = getParameterOrNull(parameters,"networkId");
        subnetId = getParameterOrNull(parameters,"subnetId");
        sharedProjectId = getParameterOrNull(parameters,"sharedProjectId");
        return (T) this;
    }
}
