package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.QueryParam;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.EffectiveScheduleParams.PARAMS)
public class MaintenanceWindowEffectiveScheduleParams {

    @NotBlank
    @QueryParam("resourceCrn")
    @Schema(description = ModelDescriptions.EffectiveScheduleParams.RESOURCE_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceCrn;

    @QueryParam("environmentCrn")
    @Schema(description = ModelDescriptions.EffectiveScheduleParams.ENVIRONMENT_CRN)
    private String environmentCrn;

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    @AssertTrue(message = "environmentCrn must not be blank when provided")
    public boolean isEnvironmentCrnValid() {
        return environmentCrn == null || !environmentCrn.isBlank();
    }

    @Override
    public String toString() {
        return "MaintenanceWindowEffectiveScheduleParams{" +
                "resourceCrn='" + resourceCrn + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                '}';
    }
}
