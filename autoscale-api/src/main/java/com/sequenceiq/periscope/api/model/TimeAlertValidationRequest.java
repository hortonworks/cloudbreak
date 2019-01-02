package com.sequenceiq.periscope.api.model;

import javax.validation.constraints.NotNull;

import com.sequenceiq.periscope.doc.ApiDescription.TimeAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class TimeAlertValidationRequest implements Json {

    @NotNull
    @ApiModelProperty(TimeAlertJsonProperties.CRON)
    private String cronExpression;

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}
