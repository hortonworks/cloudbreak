package com.sequenceiq.common.api.cloudstorage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsStorageParameters {

    private S3Guard s3Guard;

    private AwsEfsParameters efsParameters;

    public S3Guard getS3Guard() {
        return s3Guard;
    }

    public void setS3Guard(S3Guard s3Guard) {
        this.s3Guard = s3Guard;
    }

    public AwsEfsParameters getEfsParameters() {
        return efsParameters;
    }

    public void setEfsParameters(AwsEfsParameters efsParameters) {
        this.efsParameters = efsParameters;
    }
}
