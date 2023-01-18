package com.sequenceiq.common.api.telemetry.request;

import com.sequenceiq.common.api.telemetry.base.LoggingBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoggingRequest")
public class LoggingRequest extends LoggingBase {
    @Override
    public String toString() {
        return super.toString() + ", " + "LoggingRequest{}";
    }
}
