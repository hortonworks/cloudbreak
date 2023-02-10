package com.sequenceiq.periscope.api.model;

import java.util.Date;

import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class DistroXAutoscaleScalingActivityResponse implements Json {

    @Schema(description = ApiDescription.ClusterJsonsProperties.OPERATION_ID)
    private String operationId;

    @Schema(description = ApiDescription.ClusterJsonsProperties.START_TIME)
    private Date startTime;

    @Schema(description = ApiDescription.ClusterJsonsProperties.END_TIME)
    private Date endTime;

    @Schema(description = ApiDescription.ClusterJsonsProperties.ACTIVITY_STATUS)
    private ApiActivityStatus apiActivityStatus;

    @Schema(description = ApiDescription.ClusterJsonsProperties.ACTIVITY_REASON)
    private String scalingActivityReason;

    public DistroXAutoscaleScalingActivityResponse() {
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public ApiActivityStatus getActivityStatus() {
        return apiActivityStatus;
    }

    public void setActivityStatus(ApiActivityStatus apiActivityStatus) {
        this.apiActivityStatus = apiActivityStatus;
    }

    public String getScalingActivityReason() {
        return scalingActivityReason;
    }

    public void setScalingActivityReason(String scalingActivityReason) {
        this.scalingActivityReason = scalingActivityReason;
    }

}