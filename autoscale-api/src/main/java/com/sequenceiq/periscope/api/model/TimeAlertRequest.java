package com.sequenceiq.periscope.api.model;

import javax.validation.Valid;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;
import com.sequenceiq.periscope.doc.ApiDescription.TimeAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class TimeAlertRequest extends AbstractAlertJson {

    @ApiModelProperty(TimeAlertJsonProperties.TIMEZONE)
    private String timeZone;

    @ApiModelProperty(TimeAlertJsonProperties.CRON)
    private String cron;

    @Valid
    @ApiModelProperty(BaseAlertJsonProperties.SCALINGPOLICYID)
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

    public ScalingPolicyRequest getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicyRequest scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }
}
