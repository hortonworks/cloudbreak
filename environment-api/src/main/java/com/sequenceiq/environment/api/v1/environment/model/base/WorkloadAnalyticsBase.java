package com.sequenceiq.environment.api.v1.environment.model.base;

import com.sequenceiq.cloudbreak.cloud.model.WorkloadAnalyticsAttributesHolder;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class WorkloadAnalyticsBase extends CommonTelemetryFields {

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY_WA_DATABUS_ENDPOINT)
    private String databusEndpoint;

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY_WA_ATTRIBUTES)
    private WorkloadAnalyticsAttributesHolder attributes;

    public String getDatabusEndpoint() {
        return databusEndpoint;
    }

    public void setDatabusEndpoint(String databusEndpoint) {
        this.databusEndpoint = databusEndpoint;
    }

    public WorkloadAnalyticsAttributesHolder getAttributes() {
        return attributes;
    }

    public void setAttributes(WorkloadAnalyticsAttributesHolder attributes) {
        this.attributes = attributes;
    }
}
