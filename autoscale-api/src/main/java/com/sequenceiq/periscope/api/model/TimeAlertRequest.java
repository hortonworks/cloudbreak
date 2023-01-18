package com.sequenceiq.periscope.api.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;
import com.sequenceiq.periscope.doc.ApiDescription.TimeAlertJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class TimeAlertRequest extends AbstractAlertJson {

    @Schema(description = TimeAlertJsonProperties.TIMEZONE)
    @Size(max = 50)
    @NotBlank
    private String timeZone;

    @Schema(description = TimeAlertJsonProperties.CRON)
    @NotBlank
    @Size(max = 100)
    private String cron;

    @Valid
    @NotNull
    @Schema(description = BaseAlertJsonProperties.SCALINGPOLICYID)
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
