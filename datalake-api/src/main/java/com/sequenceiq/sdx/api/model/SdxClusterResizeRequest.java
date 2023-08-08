package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxClusterResizeRequest {

    @ApiModelProperty(ModelDescriptions.ENVIRONMENT_NAME)
    @NotNull
    private String environment;

    @ApiModelProperty(ModelDescriptions.CLUSTER_SHAPE)
    @NotNull
    private SdxClusterShape clusterShape;

    @ApiModelProperty(ModelDescriptions.SKIP_VALIDATION)
    private boolean skipValidation;

    @ApiModelProperty(ModelDescriptions.SKIP_ATLAS)
    private boolean skipAtlasMetadata;

    @ApiModelProperty(ModelDescriptions.SKIP_RANGER_AUDIT)
    private boolean skipRangerAudits;

    @ApiModelProperty(ModelDescriptions.SKIP_RANGER_METADATA)
    private boolean skipRangerMetadata;

    @ApiModelProperty(ModelDescriptions.MULTI_AZ_ENABLED)
    private boolean enableMultiAz;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public SdxClusterShape getClusterShape() {
        return clusterShape;
    }

    public void setClusterShape(SdxClusterShape clusterShape) {
        this.clusterShape = clusterShape;
    }

    public boolean isSkipValidation() {
        return skipValidation;
    }

    public void setSkipValidation(boolean skipValidation) {
        this.skipValidation = skipValidation;
    }

    public boolean isSkipAtlasMetadata() {
        return skipAtlasMetadata;
    }

    public void setSkipAtlasMetadata(boolean skipAtlas) {
        this.skipAtlasMetadata = skipAtlas;
    }

    public boolean isSkipRangerAudits() {
        return skipRangerAudits;
    }

    public void setSkipRangerAudits(boolean skipRangerAudits) {
        this.skipRangerAudits = skipRangerAudits;
    }

    public boolean isSkipRangerMetadata() {
        return skipRangerMetadata;
    }

    public void setSkipRangerMetadata(boolean skipRangerMetadata) {
        this.skipRangerMetadata = skipRangerMetadata;
    }

    public boolean isEnableMultiAz() {
        return enableMultiAz;
    }

    public void setEnableMultiAz(boolean enableMultiAz) {
        this.enableMultiAz = enableMultiAz;
    }

    @Override
    public String toString() {
        return "SdxClusterResizeRequest{" +
                "environment='" + environment + '\'' +
                ", clusterShape=" + clusterShape +
                ", skipValidation=" + skipValidation +
                ", skipAtlasMetadata=" + skipAtlasMetadata +
                ", skipRangerAudits=" + skipRangerAudits +
                ", skipRangerMetadata=" + skipRangerMetadata +
                ", enableMultiAz=" + enableMultiAz +
                '}';
    }
}
