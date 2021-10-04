package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Deprecated
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
        putIfValueNotNull(map, "server", server);
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
        super.parse(parameters);
        server = getParameterOrNull(parameters, "server");
    }
}
