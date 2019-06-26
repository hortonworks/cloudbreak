package com.sequenceiq.environment.api.v1.environment.model.base;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class CommonTelemetryFields {

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY_FEATURE_ENABLED)
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
