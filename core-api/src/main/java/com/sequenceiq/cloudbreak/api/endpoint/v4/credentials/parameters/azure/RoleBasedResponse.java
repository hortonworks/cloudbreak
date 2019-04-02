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
public class RoleBasedResponse extends MappableBase implements JsonEntity {

    @ApiModelProperty
    private String roleName;

    @ApiModelProperty(hidden = true)
    private String deploymentAddress;

    @ApiModelProperty
    private String spDisplayName;

    @ApiModelProperty
    private Boolean codeGrantFlow;

    @ApiModelProperty
    private String appObjectId;

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

    public String getSpDisplayName() {
        return spDisplayName;
    }

    public void setSpDisplayName(String spDisplayName) {
        this.spDisplayName = spDisplayName;
    }

    public Boolean getCodeGrantFlow() {
        return codeGrantFlow;
    }

    public void setCodeGrantFlow(Boolean codeGrantFlow) {
        this.codeGrantFlow = codeGrantFlow;
    }

    public String getAppObjectId() {
        return appObjectId;
    }

    public void setAppObjectId(String appObjectId) {
        this.appObjectId = appObjectId;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("roleName", roleName != null ? roleName : "");
        map.put("deploymentAddress", deploymentAddress);
        map.put("spDisplayName", spDisplayName);
        map.put("codeGrantFlow", codeGrantFlow);
        map.put("appObjectId", appObjectId);
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
        spDisplayName = getParameterOrNull(parameters, "spDisplayName");
        String cgf = getParameterOrNull(parameters, "codeGrantFlow");
        codeGrantFlow = "true".equalsIgnoreCase(cgf) || "false".equalsIgnoreCase(cgf) ? Boolean.valueOf(cgf) : null;
        appObjectId = getParameterOrNull(parameters, "appObjectId");
    }

}
