package com.sequenceiq.environment.api.v1.environment.model.base;

import com.sequenceiq.cloudbreak.cloud.model.LoggingAttributesHolder;
import com.sequenceiq.cloudbreak.cloud.model.LoggingOutputType;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class LoggingBase extends CommonTelemetryFields {
    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY_LOGGING_OUTPUT_TYPE)
    private LoggingOutputType output;

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY_LOGGING_ATTRIBUTES)
    private LoggingAttributesHolder attributes;

    public LoggingOutputType getOutput() {
        return output;
    }

    public void setOutput(LoggingOutputType output) {
        this.output = output;
    }

    public LoggingAttributesHolder getAttributes() {
        return attributes;
    }

    public void setAttributes(LoggingAttributesHolder attributes) {
        this.attributes = attributes;
    }
}
