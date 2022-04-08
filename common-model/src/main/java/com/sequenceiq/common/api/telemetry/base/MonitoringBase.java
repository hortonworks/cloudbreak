package com.sequenceiq.common.api.telemetry.base;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitoringBase implements Serializable {

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_MONITORING_REMOTE_WRITE_URL)
    private String remoteWriteUrl;

    public String getRemoteWriteUrl() {
        return remoteWriteUrl;
    }

    public void setRemoteWriteUrl(String remoteWriteUrl) {
        this.remoteWriteUrl = remoteWriteUrl;
    }

    @Override
    public String toString() {
        return "MonitoringBase{" +
                "remoteWriteUrl='" + remoteWriteUrl + '\'' +
                '}';
    }
}
