package com.sequenceiq.periscope.api.model;

import java.util.Date;

import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DistroXAutoscaleScalingActivityResponse implements Json {

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.TRIGGER_CRN)
    private String triggerCrn;

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.FLOW_ID)
    private String flowId;

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.START_TIME)
    private Date startTime;

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.END_TIME)
    private Date endTime;

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.TRIGGER_STATUS)
    private TriggerStatus triggerStatus;

    public DistroXAutoscaleScalingActivityResponse() {
    }

    public String getTriggerCrn() {
        return triggerCrn;
    }

    public void setTriggerCrn(String triggerCrn) {
        this.triggerCrn = triggerCrn;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
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

    public TriggerStatus getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(TriggerStatus triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

}