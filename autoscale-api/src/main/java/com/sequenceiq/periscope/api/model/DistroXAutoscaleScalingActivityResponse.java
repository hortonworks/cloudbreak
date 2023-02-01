package com.sequenceiq.periscope.api.model;

import java.util.Date;

import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DistroXAutoscaleScalingActivityResponse implements Json {

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.OPERATION_ID)
    private String operationId;

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.START_TIME)
    private Date startTime;

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.END_TIME)
    private Date endTime;

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.ACTIVITY_STATUS)
    private ApiActivityStatus apiActivityStatus;

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.ACTIVITY_REASON)
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