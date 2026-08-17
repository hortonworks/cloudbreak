package com.sequenceiq.maintenance.api.v1.schedule.model.response;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.Occurrence.RESPONSE)
public class MaintenanceWindowOccurrenceResponse {

    @Schema(description = ModelDescriptions.Occurrence.WINDOW_START)
    private Long windowStart;

    @Schema(description = ModelDescriptions.Occurrence.WINDOW_END)
    private Long windowEnd;

    public MaintenanceWindowOccurrenceResponse() {
    }

    public MaintenanceWindowOccurrenceResponse(Long windowStart, Long windowEnd) {
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
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

    @Override
    public String toString() {
        return "MaintenanceWindowOccurrenceResponse{" +
                "windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                '}';
    }
}
