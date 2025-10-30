package com.sequenceiq.distrox.api.v1.distrox.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KraftMigrationStatusResponse {
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String kraftMigrationStatus;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String recommendedAction;

    private String flowIdentifier;

    private boolean kraftMigrationRequired;

    public KraftMigrationStatusResponse(String kraftMigrationStatus, String recommendedAction, boolean kraftMigrationRequired, String flowIdentifier) {
        this.kraftMigrationStatus = kraftMigrationStatus;
        this.recommendedAction = recommendedAction;
        this.kraftMigrationRequired = kraftMigrationRequired;
        this.flowIdentifier = flowIdentifier;
    }

    public String getKraftMigrationStatus() {
        return kraftMigrationStatus;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public boolean isKraftMigrationRequired() {
        return kraftMigrationRequired;
    }

    public String getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String toString() {
        return "KraftMigrationStatusResponse{" +
                "kraftMigrationStatus=" + kraftMigrationStatus +
                ", recommendedAction='" + recommendedAction + '\'' +
                ", kraftMigrationRequired='" + kraftMigrationRequired + '\'' +
                ", flowIdentifier='" + flowIdentifier + '\'' +
                '}';
    }
}
