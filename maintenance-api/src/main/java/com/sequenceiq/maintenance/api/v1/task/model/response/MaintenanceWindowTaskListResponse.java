package com.sequenceiq.maintenance.api.v1.task.model.response;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.TaskListResponse.RESPONSE)
public class MaintenanceWindowTaskListResponse {

    private List<MaintenanceWindowTaskResponse> tasks = new ArrayList<>();

    public MaintenanceWindowTaskListResponse() {
    }

    @Schema(description = ModelDescriptions.TaskListResponse.TASKS)
    public List<MaintenanceWindowTaskResponse> getTasks() {
        return tasks;
    }

    public void setTasks(List<MaintenanceWindowTaskResponse> tasks) {
        this.tasks = tasks;
    }
}
