package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxBackupLocationValidationRequest {

    @ValidStackNameFormat
    @ValidStackNameLength
    @ApiModelProperty(ModelDescriptions.DATA_LAKE_NAME)
    private String clusterName;

    public SdxBackupLocationValidationRequest() {
    }

    public SdxBackupLocationValidationRequest(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterName() {
        return clusterName;
    }
}