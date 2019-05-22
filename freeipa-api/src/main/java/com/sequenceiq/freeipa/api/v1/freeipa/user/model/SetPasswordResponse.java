package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SetPasswordV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetPasswordResponse {

    @ApiModelProperty(value = UserModelDescriptions.SUCCESS_ENVIRONMENTNAME)
    private final List<String> success;

    @ApiModelProperty(value = UserModelDescriptions.FAILURE_ENVIRONMENTNAME)
    private final List<String> failure;

    public SetPasswordResponse(List<String> success, List<String> failure) {
        this.success = success;
        this.failure = failure;
    }

    public List<String> getSuccess() {
        return success;
    }

    public List<String> getFailure() {
        return failure;
    }
}
