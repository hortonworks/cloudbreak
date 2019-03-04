package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(parent = KeystoneV3Base.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ProjectKeystoneV3Parameters extends KeystoneV3Base {

    @NotNull
    @ApiModelProperty(required = true)
    private String projectDomainName;

    @NotNull
    @ApiModelProperty(required = true)
    private String projectName;

    public String getProjectDomainName() {
        return projectDomainName;
    }

    public void setProjectDomainName(String projectDomainName) {
        this.projectDomainName = projectDomainName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("projectDomainName", projectDomainName);
        map.put("projectName", projectName);
        map.put("keystoneAuthScope", KeystoneAuthScopes.PROJECT.getValue());
        map.put("selector", OpenstackSelector.PROJECT.getValue());
        map.put("userDomain", getUserDomain());
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
        super.parse(parameters);
        projectDomainName = getParameterOrNull(parameters, "projectDomainName");
        projectName = getParameterOrNull(parameters, "projectName");
    }

}
