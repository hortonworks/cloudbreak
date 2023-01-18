package com.sequenceiq.common.api.telemetry.response;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.telemetry.model.VmLog;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VmLogsResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
public class VmLogsResponse implements Serializable {

    private List<VmLog> logs = List.of();

    public List<VmLog> getLogs() {
        return logs;
    }

    public void setLogs(List<VmLog> logs) {
        this.logs = logs;
    }
}
