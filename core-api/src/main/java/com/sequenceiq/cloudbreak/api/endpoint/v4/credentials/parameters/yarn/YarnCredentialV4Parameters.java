package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.MappableBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class YarnCredentialV4Parameters extends MappableBase {

    @ApiModelProperty(required = true)
    private String ambariUser;

    @ApiModelProperty(required = true)
    private String endpoint;

    public String getAmbariUser() {
        return ambariUser;
    }

    public void setAmbariUser(String ambariUser) {
        this.ambariUser = ambariUser;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put("cumulusAmbariUser", ambariUser);
        map.put("yarnEndpoint", endpoint);
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.YARN;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        ambariUser = getParameterOrNull(parameters, "ambariUser");
        endpoint = getParameterOrNull(parameters, "endpoint");
    }

}
