package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import jakarta.validation.constraints.AssertTrue;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;
import com.sequenceiq.maintenance.api.v1.schedule.validation.MaintenanceScheduleRecurrenceFieldValidator;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.ScheduleUpdateRequest.REQUEST)
public class UpdateMaintenanceWindowScheduleRequest extends MaintenanceWindowScheduleFieldsRequest {

    @AssertTrue(message = "recurrenceKind requires matching schedule fields for the selected recurrence pattern "
            + "and must not include fields used by other patterns")
    public boolean isRecurrenceConfigurationValid() {
        return MaintenanceScheduleRecurrenceFieldValidator.isValidForPatch(this);
    }
}
