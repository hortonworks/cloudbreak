package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.maintenance.api.doc.ModelDescriptions;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.validation.MaintenanceScheduleRecurrenceFieldValidator;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.SchedulePreviewRequest.REQUEST)
public class MaintenanceWindowSchedulePreviewRequest extends MaintenanceWindowScheduleFieldsRequest {

    private static final int DEFAULT_LIMIT = 5;

    private static final int MAX_LIMIT = 20;

    @OneOfEnum(enumClass = MaintenanceScopeType.class, message = "Value must be one of the followings %s", fieldName = "scopeType")
    @Schema(description = ModelDescriptions.SchedulePreviewRequest.SCOPE_TYPE)
    private String scopeType;

    @Schema(description = ModelDescriptions.SchedulePreviewRequest.SCOPE_ID)
    private String scopeId;

    @Min(1)
    @Max(MAX_LIMIT)
    @Schema(description = ModelDescriptions.SchedulePreviewRequest.LIMIT)
    private Integer limit;

    @Schema(description = ModelDescriptions.SchedulePreviewRequest.INCLUDE_PERSISTED_SKIPS)
    private boolean includePersistedSkips = true;

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

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public boolean isIncludePersistedSkips() {
        return includePersistedSkips;
    }

    public void setIncludePersistedSkips(boolean includePersistedSkips) {
        this.includePersistedSkips = includePersistedSkips;
    }

    public int resolvedLimit() {
        return limit == null ? DEFAULT_LIMIT : limit;
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

    @AssertTrue(message = "scopeType and scopeId must both be provided or both omitted")
    public boolean isScopeValid() {
        boolean hasScopeType = scopeType != null && !scopeType.isBlank();
        boolean hasScopeId = scopeId != null && !scopeId.isBlank();
        return hasScopeType == hasScopeId;
    }

    @AssertTrue(message = "recurrenceKind requires matching schedule fields for the selected recurrence pattern "
            + "and must not include fields used by other patterns")
    public boolean isRecurrenceConfigurationValid() {
        return MaintenanceScheduleRecurrenceFieldValidator.isValidForCreate(this);
    }

    @Override
    public String toString() {
        return "MaintenanceWindowSchedulePreviewRequest{" +
                "scopeType='" + scopeType + '\'' +
                ", scopeId='" + scopeId + '\'' +
                ", limit=" + limit +
                ", includePersistedSkips=" + includePersistedSkips +
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
