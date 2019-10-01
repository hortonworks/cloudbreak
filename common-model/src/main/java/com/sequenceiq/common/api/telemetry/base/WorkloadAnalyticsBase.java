package com.sequenceiq.common.api.telemetry.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.telemetry.common.CommonTelemetryParams;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class WorkloadAnalyticsBase extends CommonTelemetryParams {

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_WA_DATABUS_ENDPOINT)
    private String databusEndpoint;

    public String getDatabusEndpoint() {
        return databusEndpoint;
    }

    public void setDatabusEndpoint(String databusEndpoint) {
        this.databusEndpoint = databusEndpoint;
    }
}
