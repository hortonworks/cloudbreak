package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.CredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YarnCredentialV4Parameters implements CredentialV4Parameters {

    @ApiModelProperty(required = true)
    private String ambariUser;

    @ApiModelProperty(required = true)
    private String endpoint;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.YARN;
    }

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
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cumulusAmbariUser", ambariUser);
        map.put("yarnEndpoint", endpoint);
        return map;
    }

}
