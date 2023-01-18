package com.sequenceiq.common.api.telemetry.base;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitoringBase implements Serializable {

    @Schema(description = TelemetryModelDescription.TELEMETRY_MONITORING_REMOTE_WRITE_URL)
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
