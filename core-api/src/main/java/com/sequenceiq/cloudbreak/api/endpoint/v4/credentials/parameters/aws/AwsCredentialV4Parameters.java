package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.CredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsCredentialV4Parameters implements CredentialV4Parameters {

    @ApiModelProperty
    private KeyBasedCredentialParameters keyBased;

    @ApiModelProperty
    private RoleBasedCredentialParameters roleBased;

    @NotNull
    @ApiModelProperty(required = true)
    private Boolean govCloud = false;

    public KeyBasedCredentialParameters getKeyBased() {
        return keyBased;
    }

    public RoleBasedCredentialParameters getRoleBased() {
        return roleBased;
    }

    public void setKeyBased(KeyBasedCredentialParameters keyBased) {
        this.keyBased = keyBased;
    }

    public void setRoleBased(RoleBasedCredentialParameters roleBased) {
        this.roleBased = roleBased;
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
        if (keyBased != null) {
            map.putAll(keyBased.asMap());
        } else if (roleBased != null) {
            map.putAll(roleBased.asMap());
        }
        return map;
    }
}
