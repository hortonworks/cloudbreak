package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws;

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
public class AwsCredentialV4Parameters extends MappableBase {

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
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put("govCloud", govCloud);
        if (keyBased != null) {
            map.putAll(keyBased.asMap());
        } else if (roleBased != null) {
            map.putAll(roleBased.asMap());
        }
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        keyBased = new KeyBasedCredentialParameters();
        keyBased.parse(parameters);
    }

}
