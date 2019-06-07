package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.telemetry.TelemetryComponentParams;
import com.sequenceiq.cloudbreak.cloud.model.LoggingAttributesHolder;
import com.sequenceiq.cloudbreak.cloud.model.LoggingOutputType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoggingV4Base extends TelemetryComponentParams {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_LOGGING_OUTPUT_TYPE)
    private LoggingOutputType output;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_COMPONENT_ATTRIBUTES)
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
