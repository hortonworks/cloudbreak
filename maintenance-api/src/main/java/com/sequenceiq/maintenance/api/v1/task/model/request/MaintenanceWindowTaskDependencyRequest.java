package com.sequenceiq.maintenance.api.v1.task.model.request;

import jakarta.validation.constraints.NotBlank;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.TaskDependency.REQUEST)
public class MaintenanceWindowTaskDependencyRequest {

    @NotBlank
    @Schema(description = ModelDescriptions.TaskDependency.RESOURCE_CRN)
    private String resourceCrn;

    @NotBlank
    @Schema(description = ModelDescriptions.TaskDependency.TASK_TYPE)
    private String taskType;

    @NotBlank
    @Schema(description = ModelDescriptions.TaskDependency.WORK_ITEM_ID)
    private String workItemId;

    public MaintenanceWindowTaskDependencyRequest() {
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getWorkItemId() {
        return workItemId;
    }

    public void setWorkItemId(String workItemId) {
        this.workItemId = workItemId;
    }
}
