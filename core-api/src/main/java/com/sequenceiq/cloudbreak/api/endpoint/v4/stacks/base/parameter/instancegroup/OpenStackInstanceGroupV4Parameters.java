package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4ParametersBase;

import io.swagger.annotations.ApiModelProperty;

public class OpenStackInstanceGroupV4Parameters extends InstanceGroupV4ParametersBase {

    @ApiModelProperty
    private String server;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put("server", server);
        return map;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        setDiscoveryName(getParameterOrNull(parameters, "discoveryName"));
        setInstanceName(getParameterOrNull(parameters, "instanceName"));
        server = getParameterOrNull(parameters, "server");
    }
}
