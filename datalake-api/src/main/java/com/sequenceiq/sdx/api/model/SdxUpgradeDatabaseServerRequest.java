package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxUpgradeDatabaseServerRequest {

    @ApiModelProperty(ModelDescriptions.TARGET_MAJOR_VERSION)
    private TargetMajorVersion targetMajorVersion;

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    public void setTargetMajorVersion(TargetMajorVersion targetMajorVersion) {
        this.targetMajorVersion = targetMajorVersion;
    }

    @Override
    public String toString() {
        return "SdxUpgradeRdsRequest{" +
                "targetVersion=" + targetMajorVersion +
                '}';
    }
}
