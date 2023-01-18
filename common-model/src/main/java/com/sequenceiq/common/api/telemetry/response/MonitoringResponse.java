package com.sequenceiq.common.api.telemetry.response;

import com.sequenceiq.common.api.telemetry.base.MonitoringBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MonitoringResponse")
public class MonitoringResponse extends MonitoringBase {

    @Override
    public String toString() {
        return "MonitoringResponse{} " + super.toString();
    }
}
