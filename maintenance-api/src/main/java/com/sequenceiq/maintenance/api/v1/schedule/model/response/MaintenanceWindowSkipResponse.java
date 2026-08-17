package com.sequenceiq.maintenance.api.v1.schedule.model.response;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.SkipResponse.RESPONSE)
public class MaintenanceWindowSkipResponse {

    @Schema(description = ModelDescriptions.SkipResponse.ID)
    private Long id;

    @Schema(description = ModelDescriptions.SkipResponse.MAINTENANCE_WINDOW_SCHEDULE_ID)
    private Long maintenanceWindowScheduleId;

    @Schema(description = ModelDescriptions.SkipResponse.WINDOW_START)
    private Long windowStart;

    @Schema(description = ModelDescriptions.SkipResponse.WINDOW_END)
    private Long windowEnd;

    @Schema(description = ModelDescriptions.SkipResponse.TIMEZONE)
    private String timezone;

    @Schema(description = ModelDescriptions.SkipResponse.CREATED_AT)
    private Long createdAt;

    @Schema(description = ModelDescriptions.SkipResponse.CREATED_BY)
    private String createdBy;

    @Schema(description = ModelDescriptions.SkipResponse.REASON)
    private String reason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMaintenanceWindowScheduleId() {
        return maintenanceWindowScheduleId;
    }

    public void setMaintenanceWindowScheduleId(Long maintenanceWindowScheduleId) {
        this.maintenanceWindowScheduleId = maintenanceWindowScheduleId;
    }

    public Long getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(Long windowStart) {
        this.windowStart = windowStart;
    }

    public Long getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Long windowEnd) {
        this.windowEnd = windowEnd;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowSkipResponse{" +
                "id=" + id +
                ", maintenanceWindowScheduleId=" + maintenanceWindowScheduleId +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", timezone='" + timezone + '\'' +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
