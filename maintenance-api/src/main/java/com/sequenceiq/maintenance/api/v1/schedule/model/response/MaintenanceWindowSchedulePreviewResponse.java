package com.sequenceiq.maintenance.api.v1.schedule.model.response;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.SchedulePreviewResponse.RESPONSE)
public class MaintenanceWindowSchedulePreviewResponse {

    @Schema(description = ModelDescriptions.ScheduleFields.TIMEZONE)
    private String timezone;

    @Schema(description = ModelDescriptions.SchedulePreviewResponse.OCCURRENCES)
    private List<MaintenanceWindowOccurrencePreviewResponse> occurrences = new ArrayList<>();

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public List<MaintenanceWindowOccurrencePreviewResponse> getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(List<MaintenanceWindowOccurrencePreviewResponse> occurrences) {
        this.occurrences = occurrences;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowSchedulePreviewResponse{" +
                "timezone='" + timezone + '\'' +
                ", occurrences=" + occurrences +
                '}';
    }
}
