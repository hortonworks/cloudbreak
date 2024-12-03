package com.sequenceiq.periscope.api.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class AbstractAlertJson implements Json {

    @Pattern(regexp = "(^[a-zA-Z][-a-zA-Z0-9]*$)",
            message = "The name can only contain alphanumeric characters and hyphens and has to start with an alphabetic character")
    @NotEmpty
    @Size(max = 250)
    @Schema(description = BaseAlertJsonProperties.ALERTNAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String alertName;

    @Schema(description = BaseAlertJsonProperties.DESCRIPTION)
    @Size(max = 250)
    private String description;

    public String getAlertName() {
        return alertName;
    }

    public void setAlertName(String alertName) {
        this.alertName = alertName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
