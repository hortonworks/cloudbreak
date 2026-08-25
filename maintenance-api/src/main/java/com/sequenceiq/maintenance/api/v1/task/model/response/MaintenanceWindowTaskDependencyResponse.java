package com.sequenceiq.maintenance.api.v1.task.model.response;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.TaskDependency.RESPONSE)
public class MaintenanceWindowTaskDependencyResponse {

    @Schema(description = ModelDescriptions.TaskDependency.RESOURCE_CRN)
    private String resourceCrn;

    @Schema(description = ModelDescriptions.TaskDependency.TASK_TYPE)
    private String taskType;

    @Schema(description = ModelDescriptions.TaskDependency.WORK_ITEM_ID)
    private String workItemId;

    public MaintenanceWindowTaskDependencyResponse() {
    }

    public MaintenanceWindowTaskDependencyResponse(String resourceCrn, String taskType, String workItemId) {
        this.resourceCrn = resourceCrn;
        this.taskType = taskType;
        this.workItemId = workItemId;
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
