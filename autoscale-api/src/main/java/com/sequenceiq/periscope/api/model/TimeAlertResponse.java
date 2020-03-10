package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;
import com.sequenceiq.periscope.doc.ApiDescription.TimeAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class TimeAlertResponse extends AbstractAlertJson {

    @ApiModelProperty(BaseAlertJsonProperties.ID)
    private Long id;

    @ApiModelProperty(TimeAlertJsonProperties.TIMEZONE)
    private String timeZone;

    @ApiModelProperty(TimeAlertJsonProperties.CRON)
    private String cron;

    @ApiModelProperty(BaseAlertJsonProperties.SCALINGPOLICYID)
    private ScalingPolicyResponse scalingPolicy;

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

    public ScalingPolicyResponse getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicyResponse scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }
}
