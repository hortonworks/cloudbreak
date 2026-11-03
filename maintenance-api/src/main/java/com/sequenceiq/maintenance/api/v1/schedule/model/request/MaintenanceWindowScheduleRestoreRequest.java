package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.ScheduleRestoreRequest.REQUEST)
public class MaintenanceWindowScheduleRestoreRequest extends MaintenanceWindowScheduleRequest {

    @NotNull
    @Schema(description = ModelDescriptions.ScheduleRestoreRequest.ID)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowScheduleRestoreRequest{" +
                "id=" + id +
                ", " + super.toString() +
                '}';
    }
}
