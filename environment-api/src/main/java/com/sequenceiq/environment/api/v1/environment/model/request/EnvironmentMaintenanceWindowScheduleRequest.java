package com.sequenceiq.environment.api.v1.environment.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleFieldsRequest;
import com.sequenceiq.maintenance.api.v1.schedule.validation.MaintenanceScheduleRecurrenceFieldValidator;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Optional environment-scoped maintenance window override on create/edit. Persisted by the maintenance service, not Environment DB.
 */
@Schema(name = "EnvironmentMaintenanceWindowScheduleV1Request")
public class EnvironmentMaintenanceWindowScheduleRequest extends MaintenanceWindowScheduleFieldsRequest {

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
