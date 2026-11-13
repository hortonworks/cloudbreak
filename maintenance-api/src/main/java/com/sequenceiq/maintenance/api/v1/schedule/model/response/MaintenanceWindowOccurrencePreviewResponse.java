package com.sequenceiq.maintenance.api.v1.schedule.model.response;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.OccurrencePreview.RESPONSE)
public class MaintenanceWindowOccurrencePreviewResponse {

    @Schema(description = ModelDescriptions.Occurrence.WINDOW_START)
    private Long windowStart;

    @Schema(description = ModelDescriptions.Occurrence.WINDOW_END)
    private Long windowEnd;

    @Schema(description = ModelDescriptions.OccurrencePreview.SKIPPED)
    private boolean skipped;

    public MaintenanceWindowOccurrencePreviewResponse() {
    }

    public MaintenanceWindowOccurrencePreviewResponse(Long windowStart, Long windowEnd, boolean skipped) {
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.skipped = skipped;
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

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowOccurrencePreviewResponse{" +
                "windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", skipped=" + skipped +
                '}';
    }
}
