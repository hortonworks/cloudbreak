package com.sequenceiq.periscope.api.model;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;
import com.sequenceiq.periscope.doc.ApiDescription.TimeAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class TimeAlertRequest extends AbstractAlertJson {

    @ApiModelProperty(TimeAlertJsonProperties.TIMEZONE)
    @Size(max = 50)
    private String timeZone;

    @ApiModelProperty(TimeAlertJsonProperties.CRON)
    @NotEmpty
    @Size(max = 100)
    private String cron;

    @Valid
    @NotNull
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
