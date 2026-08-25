package com.sequenceiq.maintenance.api.v1.task.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.ws.rs.QueryParam;

import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.maintenance.api.doc.ModelDescriptions;
import com.sequenceiq.maintenance.api.model.MaintenanceTaskKind;
import com.sequenceiq.maintenance.api.model.MaintenanceTaskStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.TaskListParams.PARAMS)
public class MaintenanceWindowTaskListParams {

    @QueryParam("resourceCrn")
    @Schema(description = ModelDescriptions.TaskListParams.RESOURCE_CRN)
    private String resourceCrn;

    @QueryParam("environmentCrn")
    @Schema(description = ModelDescriptions.TaskListParams.ENVIRONMENT_CRN)
    private String environmentCrn;

    @QueryParam("taskType")
    @Schema(description = ModelDescriptions.TaskListParams.TASK_TYPE)
    private String taskType;

    @QueryParam("workItemId")
    @Schema(description = ModelDescriptions.TaskListParams.WORK_ITEM_ID)
    private String workItemId;

    @QueryParam("taskKind")
    @OneOfEnum(enumClass = MaintenanceTaskKind.class, message = "Value must be one of the followings %s", fieldName = "taskKind")
    @Schema(description = ModelDescriptions.TaskListParams.TASK_KIND)
    private String taskKind;

    @QueryParam("status")
    @OneOfEnum(enumClass = MaintenanceTaskStatus.class, message = "Value must be one of the followings %s", fieldName = "status")
    @Schema(description = ModelDescriptions.TaskListParams.STATUS)
    private String status;

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
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

    public String getTaskKind() {
        return taskKind;
    }

    public void setTaskKind(String taskKind) {
        this.taskKind = taskKind;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @AssertTrue(message = "taskType requires resourceCrn")
    public boolean isTaskTypeFilterValid() {
        return isBlank(taskType) || !isBlank(resourceCrn);
    }

    @AssertTrue(message = "workItemId requires taskType and resourceCrn")
    public boolean isWorkItemIdFilterValid() {
        return isBlank(workItemId) || (!isBlank(taskType) && !isBlank(resourceCrn));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
