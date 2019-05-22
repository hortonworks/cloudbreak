package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SetPasswordV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetPasswordRequest {

    @ApiModelProperty(value = UserModelDescriptions.USER_PASSWORD)
    private String password;

    public String getPassword() {
        return password;
    }
}
