package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription;
import com.sequenceiq.periscope.doc.ApiDescription.TimeAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("TimeAlertResponse")
public class TimeAlertResponse extends AbstractAlertJson {

    @ApiModelProperty(ApiDescription.BaseAlertJsonProperties.ID)
    private Long id;

    @ApiModelProperty(TimeAlertJsonProperties.TIMEZONE)
    private String timeZone;

    @ApiModelProperty(TimeAlertJsonProperties.CRON)
    private String cron;

    @ApiModelProperty(ApiDescription.BaseAlertJsonProperties.SCALINGPOLICYID)
    private Long scalingPolicyId;

    @ApiModelProperty(ApiDescription.BaseAlertJsonProperties.SCALINGPOLICYID)
    private ScalingPolicyRequest scalingPolicy;

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScalingPolicyId() {
        return scalingPolicyId;
    }

    public void setScalingPolicyId(Long scalingPolicyId) {
        this.scalingPolicyId = scalingPolicyId;
    }

    public ScalingPolicyRequest getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicyRequest scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }
}
