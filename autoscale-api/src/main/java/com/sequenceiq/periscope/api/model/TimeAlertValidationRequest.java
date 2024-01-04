package com.sequenceiq.periscope.api.model;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.periscope.doc.ApiDescription.TimeAlertJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class TimeAlertValidationRequest implements Json {

    @NotNull
    @Schema(description = TimeAlertJsonProperties.CRON)
    private String cronExpression;

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}
