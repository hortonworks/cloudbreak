package com.sequenceiq.common.api.telemetry.request;

import com.sequenceiq.common.api.telemetry.base.WorkloadAnalyticsBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WorkloadAnalyticsRequest")
public class WorkloadAnalyticsRequest extends WorkloadAnalyticsBase {
    @Override
    public String toString() {
        return super.toString() + ", " + "WorkloadAnalyticsRequest{}";
    }
}
