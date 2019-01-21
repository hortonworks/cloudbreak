package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.CredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AwsCredentialV4Parameters implements CredentialV4Parameters {

    @ApiModelProperty
    private KeyBasedCredentialParameters keyBasedCredentialParameters;

    @ApiModelProperty
    private RoleBasedCredentialParameters roleBasedCredentialParameters;

    @NotNull
    @ApiModelProperty(required = true)
    private Boolean govCloud = false;

    public KeyBasedCredentialParameters getKeyBasedCredentialParameters() {
        return keyBasedCredentialParameters;
    }

    public RoleBasedCredentialParameters getRoleBasedCredentialParameters() {
        return roleBasedCredentialParameters;
    }

    public void setKeyBasedCredentialParameters(KeyBasedCredentialParameters keyBasedCredentialParameters) {
        this.keyBasedCredentialParameters = keyBasedCredentialParameters;
    }

    public void setRoleBasedCredentialParameters(RoleBasedCredentialParameters roleBasedCredentialParameters) {
        this.roleBasedCredentialParameters = roleBasedCredentialParameters;
    }

    public boolean getGovCloud() {
        return govCloud;
    }

    public void setGovCloud(Boolean govCloud) {
        this.govCloud = govCloud;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("govCloud", govCloud);
        if (keyBasedCredentialParameters != null) {
            map.putAll(keyBasedCredentialParameters.asMap());
        } else if (roleBasedCredentialParameters != null) {
            map.putAll(roleBasedCredentialParameters.asMap());
        }
        return map;
    }
}
