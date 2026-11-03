package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Partial maintenance window schedule update on environment edit (PATCH semantics toward the maintenance service).
 */
@Schema(name = "EnvironmentMaintenanceWindowScheduleEditV1Request")
public class EnvironmentMaintenanceWindowScheduleEditRequest extends UpdateMaintenanceWindowScheduleRequest {

    @JsonIgnore
    @Override
    public boolean isRecurrenceConfigurationValid() {
        return super.isRecurrenceConfigurationValid();
    }
}
