package com.sequenceiq.common.api.telemetry.request;

import com.sequenceiq.common.api.telemetry.base.LoggingBase;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "LoggingRequest")
public class LoggingRequest extends LoggingBase {
    @Override
    public String toString() {
        return super.toString() + ", " + "LoggingRequest{}";
    }
}
