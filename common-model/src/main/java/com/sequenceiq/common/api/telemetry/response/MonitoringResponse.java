package com.sequenceiq.common.api.telemetry.response;

import com.sequenceiq.common.api.telemetry.base.MonitoringBase;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "MonitoringResponse")
public class MonitoringResponse extends MonitoringBase {

    @Override
    public String toString() {
        return "MonitoringResponse{} " + super.toString();
    }
}
