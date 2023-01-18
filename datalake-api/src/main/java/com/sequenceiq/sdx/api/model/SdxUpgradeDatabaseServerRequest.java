package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxUpgradeDatabaseServerRequest {

    @Schema(description = ModelDescriptions.TARGET_MAJOR_VERSION)
    private TargetMajorVersion targetMajorVersion;

    @Schema(description = ModelDescriptions.FORCED)
    private Boolean forced;

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    public void setTargetMajorVersion(TargetMajorVersion targetMajorVersion) {
        this.targetMajorVersion = targetMajorVersion;
    }

    public Boolean getForced() {
        return forced;
    }

    public void setForced(Boolean forced) {
        this.forced = forced;
    }

    @Override
    public String toString() {
        return "SdxUpgradeDatabaseServerRequest{" +
                "targetMajorVersion=" + targetMajorVersion +
                ", forced=" + forced +
                '}';
    }
}
