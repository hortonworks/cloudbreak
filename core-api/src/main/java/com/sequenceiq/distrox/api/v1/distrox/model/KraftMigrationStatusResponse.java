package com.sequenceiq.distrox.api.v1.distrox.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.AllowedEnumValuesAsStrings;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KraftMigrationStatusResponse {
    @NotNull
    @AllowedEnumValuesAsStrings(KraftMigrationOperationStatus.class)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String kraftMigrationStatus;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String recommendedAction;

    private boolean kraftMigrationRequired;

    public KraftMigrationStatusResponse(String kraftMigrationStatus, String recommendedAction, boolean kraftMigrationRequired) {
        this.kraftMigrationStatus = kraftMigrationStatus;
        this.recommendedAction = recommendedAction;
        this.kraftMigrationRequired = kraftMigrationRequired;
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

    @Override
    public String toString() {
        return "KraftMigrationStatusResponse{" +
                "kraftMigrationStatus=" + kraftMigrationStatus +
                ", recommendedAction='" + recommendedAction + '\'' +
                ", kraftMigrationRequired='" + kraftMigrationRequired + '\'' +
                '}';
    }
}
