package com.sequenceiq.environment.experience.policy.response;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ExperiencePolicyResponse implements Serializable {

    @ApiModelProperty(value = "AWS credential prerequisite policy JSON encoded in base64")
    private String aws;

    @ApiModelProperty(value = "AZURE credential prerequisite policy JSON encoded in base64")
    private String azure;

    public String getAws() {
        return aws;
    }

    public void setAws(String aws) {
        this.aws = aws;
    }

    public String getAzure() {
        return azure;
    }

    public void setAzure(String azure) {
        this.azure = azure;
    }

}
