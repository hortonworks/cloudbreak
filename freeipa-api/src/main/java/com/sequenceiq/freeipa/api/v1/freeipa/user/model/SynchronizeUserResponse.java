package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SynchronizeUserV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SynchronizeUserResponse {

    @ApiModelProperty(value = UserModelDescriptions.SUCCESS_ENVIRONMENTNAME)
    private final List<String> success;

    @ApiModelProperty(value = UserModelDescriptions.FAILURE_ENVIRONMENTNAME)
    private final Map<String, String> failure;

    public SynchronizeUserResponse(List<String> success, Map<String, String> failure) {
        this.success = success;
        this.failure = failure;
    }

    public List<String> getSuccess() {
        return success;
    }

    public Map<String, String> getFailure() {
        return failure;
    }
}
