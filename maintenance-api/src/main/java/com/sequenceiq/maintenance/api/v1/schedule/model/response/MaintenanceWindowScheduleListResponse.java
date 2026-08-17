package com.sequenceiq.maintenance.api.v1.schedule.model.response;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.ScheduleListResponse.RESPONSE)
public class MaintenanceWindowScheduleListResponse {

    @Schema(description = ModelDescriptions.ScheduleListResponse.SCHEDULES)
    private List<MaintenanceWindowScheduleResponse> schedules = new ArrayList<>();

    public List<MaintenanceWindowScheduleResponse> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<MaintenanceWindowScheduleResponse> schedules) {
        this.schedules = schedules;
    }
}
