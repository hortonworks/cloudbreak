package com.sequenceiq.maintenance.api.v1.schedule.model.response;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.EffectiveScheduleResponse.RESPONSE)
public class MaintenanceWindowEffectiveScheduleResponse {

    @Schema(description = ModelDescriptions.EffectiveScheduleResponse.SCHEDULE)
    private MaintenanceWindowScheduleResponse schedule;

    @Schema(description = ModelDescriptions.EffectiveScheduleResponse.SOURCE_SCOPE_TYPE)
    private String sourceScopeType;

    @Schema(description = ModelDescriptions.EffectiveScheduleResponse.SOURCE_SCOPE_ID)
    private String sourceScopeId;

    public MaintenanceWindowScheduleResponse getSchedule() {
        return schedule;
    }

    public void setSchedule(MaintenanceWindowScheduleResponse schedule) {
        this.schedule = schedule;
    }

    public String getSourceScopeType() {
        return sourceScopeType;
    }

    public void setSourceScopeType(String sourceScopeType) {
        this.sourceScopeType = sourceScopeType;
    }

    public String getSourceScopeId() {
        return sourceScopeId;
    }

    public void setSourceScopeId(String sourceScopeId) {
        this.sourceScopeId = sourceScopeId;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowEffectiveScheduleResponse{" +
                "schedule=" + schedule +
                ", sourceScopeType='" + sourceScopeType + '\'' +
                ", sourceScopeId='" + sourceScopeId + '\'' +
                '}';
    }
}
