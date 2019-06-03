package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.telemetry;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public class TelemetryComponentParams implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_COMPONENT_ENABLED)
    private boolean enabled;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_COMPONENT_ATTRIBUTES)
    private Map<String, Object> attributes;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
