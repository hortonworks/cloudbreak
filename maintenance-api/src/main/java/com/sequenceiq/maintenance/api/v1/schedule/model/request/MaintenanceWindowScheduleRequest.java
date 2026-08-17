package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.maintenance.api.doc.ModelDescriptions;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.validation.MaintenanceScheduleRecurrenceFieldValidator;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.ScheduleRequest.REQUEST)
public class MaintenanceWindowScheduleRequest extends MaintenanceWindowScheduleFieldsRequest {

    @NotBlank
    @OneOfEnum(enumClass = MaintenanceScopeType.class, message = "Value must be one of the followings %s", fieldName = "scopeType")
    @Schema(description = ModelDescriptions.ScheduleRequest.SCOPE_TYPE)
    private String scopeType;

    @NotBlank
    @Schema(description = ModelDescriptions.ScheduleRequest.SCOPE_ID)
    private String scopeId;

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

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

    @AssertTrue(message = "recurrenceKind requires matching schedule fields for the selected recurrence pattern "
            + "and must not include fields used by other patterns")
    public boolean isRecurrenceConfigurationValid() {
        return MaintenanceScheduleRecurrenceFieldValidator.isValidForCreate(this);
    }

    @Override
    public String toString() {
        return "MaintenanceWindowScheduleRequest{" +
                "scopeType='" + scopeType + '\'' +
                ", scopeId='" + scopeId + '\'' +
                ", name='" + getName() + '\'' +
                ", recurrenceKind='" + getRecurrenceKind() + '\'' +
                ", timezone='" + getTimezone() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", durationMinutes=" + getDurationMinutes() +
                ", startLocalTime='" + getStartLocalTime() + '\'' +
                ", dayOfWeek='" + getDayOfWeek() + '\'' +
                ", weekOrdinal=" + getWeekOrdinal() +
                ", dayOfMonth=" + getDayOfMonth() +
                ", cronExpression='" + getCronExpression() + '\'' +
                '}';
    }
}
