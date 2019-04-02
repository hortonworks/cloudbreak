package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure;

import java.util.LinkedHashMap;
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
public class RoleBasedRequest extends MappableBase implements JsonEntity {

    @ApiModelProperty(required = true)
    private String roleName;

    @ApiModelProperty(hidden = true)
    private String deploymentAddress;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDeploymentAddress() {
        return deploymentAddress;
    }

    public void setDeploymentAddress(String deploymentAddress) {
        this.deploymentAddress = deploymentAddress;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("roleName", roleName != null ? roleName : "");
        map.put("deploymentAddress", deploymentAddress);
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
        super.parse(parameters);
        roleName = getParameterOrNull(parameters, "roleName");
        deploymentAddress = getParameterOrNull(parameters, "deploymentAddress");
    }

}
