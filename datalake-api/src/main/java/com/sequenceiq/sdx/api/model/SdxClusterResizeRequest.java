package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxClusterResizeRequest {

    @NotNull
    @Schema(description = ModelDescriptions.ENVIRONMENT_NAME)
    private String environment;

    @NotNull
    @Schema(description = ModelDescriptions.CLUSTER_SHAPE)
    private SdxClusterShape clusterShape;

    @Schema(description = ModelDescriptions.SKIP_VALIDATION)
    private boolean skipValidation;

    @Schema(description = ModelDescriptions.SKIP_ATLAS)
    private boolean skipAtlasMetadata;

    @Schema(description = ModelDescriptions.SKIP_RANGER_AUDIT)
    private boolean skipRangerAudits;

    @Schema(description = ModelDescriptions.SKIP_RANGER_METADATA)
    private boolean skipRangerMetadata;

    @Schema(description = ModelDescriptions.MULTI_AZ_ENABLED)
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
