package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.telemetry.TelemetryComponentParams;
import com.sequenceiq.cloudbreak.cloud.model.WorkloadAnalyticsAttributesHolder;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkloadAnalyticsV4Base extends TelemetryComponentParams {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_WA_DATABUS_ENDPOINT)
    private String databusEndpoint;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_COMPONENT_ATTRIBUTES)
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
