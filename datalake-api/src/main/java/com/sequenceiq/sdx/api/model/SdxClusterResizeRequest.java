package com.sequenceiq.sdx.api.model;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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

    @Valid
    @Schema(description = ModelDescriptions.CUSTOM_INSTANCE_GROUP_OPTIONS)
    private List<SdxInstanceGroupRequest> customInstanceGroups;

    @Valid
    @Schema(description = ModelDescriptions.INSTANCE_DISK_SIZE)
    private List<SdxInstanceGroupDiskRequest> customInstanceGroupDiskSize;

    @Valid
    @Schema(description = ModelDescriptions.DATABASE)
    private SdxDatabaseComputeStorageRequest customSdxDatabaseComputeStorage;

    @Schema(description = ModelDescriptions.VALIDATION_ONLY)
    private boolean validationOnly;

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

    public List<SdxInstanceGroupRequest> getCustomInstanceGroups() {
        return customInstanceGroups;
    }

    public void setCustomInstanceGroups(List<SdxInstanceGroupRequest> customInstanceGroups) {
        this.customInstanceGroups = customInstanceGroups;
    }

    public List<SdxInstanceGroupDiskRequest> getCustomInstanceGroupDiskSize() {
        return customInstanceGroupDiskSize;
    }

    public void setCustomInstanceGroupDiskSize(List<SdxInstanceGroupDiskRequest> customInstanceGroupDiskSize) {
        this.customInstanceGroupDiskSize = customInstanceGroupDiskSize;
    }

    public SdxDatabaseComputeStorageRequest getCustomSdxDatabaseComputeStorage() {
        return customSdxDatabaseComputeStorage;
    }

    public void setCustomSdxDatabaseComputeStorage(SdxDatabaseComputeStorageRequest customSdxDatabaseComputeStorage) {
        this.customSdxDatabaseComputeStorage = customSdxDatabaseComputeStorage;
    }

    public boolean isValidationOnly() {
        return validationOnly;
    }

    public void setValidationOnly(boolean validationOnly) {
        this.validationOnly = validationOnly;
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
            ", customInstanceGroups=" + customInstanceGroups +
            ", customInstanceGroupDiskSize=" + customInstanceGroupDiskSize +
            ", customSdxDatabaseComputeStorage=" + customSdxDatabaseComputeStorage +
            ", validationOnly=" + validationOnly +
            '}';
    }
}
