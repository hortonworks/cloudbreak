package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.MappableBase;

import io.swagger.annotations.ApiModelProperty;

public abstract class InstanceGroupV4ParametersBase extends MappableBase implements JsonEntity {

    @ApiModelProperty
    private String discoveryName;

    @ApiModelProperty
    private String instanceName;

    public String getDiscoveryName() {
        return discoveryName;
    }

    public void setDiscoveryName(String discoveryName) {
        this.discoveryName = discoveryName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        discoveryName = getParameterOrNull(parameters, "discoveryName");
        instanceName = getParameterOrNull(parameters, "instanceName");
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put("discoveryName", discoveryName);
        map.put("instanceName", instanceName);
        return map;
    }
}
