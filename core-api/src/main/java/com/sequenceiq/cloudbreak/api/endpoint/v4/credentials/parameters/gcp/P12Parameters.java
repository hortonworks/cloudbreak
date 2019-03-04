package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

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
public class P12Parameters extends MappableBase {

    @NotNull
    @ApiModelProperty(required = true)
    private String projectId;

    @NotNull
    @ApiModelProperty(required = true, example = "serviceaccountemailaddress@example.com")
    private String serviceAccountId;

    @NotNull
    @ApiModelProperty(required = true)
    private String serviceAccountPrivateKey;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getServiceAccountId() {
        return serviceAccountId;
    }

    public void setServiceAccountId(String serviceAccountId) {
        this.serviceAccountId = serviceAccountId;
    }

    public String getServiceAccountPrivateKey() {
        return serviceAccountPrivateKey;
    }

    public void setServiceAccountPrivateKey(String serviceAccountPrivateKey) {
        this.serviceAccountPrivateKey = serviceAccountPrivateKey;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("projectId", projectId);
        map.put("serviceAccountId", serviceAccountId);
        map.put("serviceAccountPrivateKey", serviceAccountPrivateKey);
        map.put("selector", GcpSelectorType.P12.getName());
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        projectId = getParameterOrNull(parameters, "projectId");
        serviceAccountId = getParameterOrNull(parameters, "serviceAccountId");
        serviceAccountPrivateKey = getParameterOrNull(parameters, "serviceAccountPrivateKey");
    }

}
