package com.sequenceiq.common.api.telemetry.request;

import com.sequenceiq.common.api.telemetry.base.MonitoringBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MonitoringRequest")
public class MonitoringRequest extends MonitoringBase {

    @Override
    public String toString() {
        return "MonitoringRequest{} " + super.toString();
    }
}
