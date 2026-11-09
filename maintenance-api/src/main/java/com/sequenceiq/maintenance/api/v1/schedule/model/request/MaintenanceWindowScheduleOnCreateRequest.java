package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.maintenance.api.v1.schedule.validation.MaintenanceScheduleRecurrenceFieldValidator;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shared validation for optional nested maintenance window schedules on resource create requests.
 * Service APIs may extend this type (or embed it) to publish a distinct OpenAPI schema name per resource.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "MaintenanceWindowScheduleOnCreateRequest")
public class MaintenanceWindowScheduleOnCreateRequest extends MaintenanceWindowScheduleFieldsRequest {

    @Override
    @NotNull
    public String getRecurrenceKind() {
        return super.getRecurrenceKind();
    }

    @Override
    @NotNull
    public Integer getDurationMinutes() {
        return super.getDurationMinutes();
    }

    @JsonIgnore
    @AssertTrue(message = "recurrenceKind requires matching schedule fields for the selected recurrence pattern "
            + "and must not include fields used by other patterns")
    public boolean isRecurrenceConfigurationValid() {
        return MaintenanceScheduleRecurrenceFieldValidator.isValidForCreate(this);
    }
}
