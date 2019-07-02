package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.HashSet;
import java.util.Set;

@ApiModel("SetPasswordV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetPasswordRequest {

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ENVIRONMENTS)
    private Set<String> environments = new HashSet<>();

    @ApiModelProperty(value = UserModelDescriptions.USER_PASSWORD)
    private String password;

    public Set<String> getEnvironments() {
        return environments;
    }

    public String getPassword() {
        return password;
    }
}
