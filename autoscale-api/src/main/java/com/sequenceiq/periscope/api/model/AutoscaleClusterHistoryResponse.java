package com.sequenceiq.periscope.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.periscope.doc.ApiDescription.HistoryJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class AutoscaleClusterHistoryResponse implements Json {

    @Schema(description = HistoryJsonProperties.CBSTACKCRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String stackCrn;

    @Schema(description = HistoryJsonProperties.SCALINGSTATUS)
    private ScalingStatus scalingStatus;

    @Schema(description = HistoryJsonProperties.STATUSREASON)
    private String statusReason;

    @Schema(description = HistoryJsonProperties.TIMESTAMP, requiredMode = Schema.RequiredMode.REQUIRED)
    private long timestamp;

    @Schema(description = HistoryJsonProperties.SCALINGACTIVITY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AutoscaleClusterHistoryActivity autoscaleClusterHistoryActivity;

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }

    public ScalingStatus getScalingStatus() {
        return scalingStatus;
    }

    public void setScalingStatus(ScalingStatus scalingStatus) {
        this.scalingStatus = scalingStatus;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public AutoscaleClusterHistoryActivity getAutoscaleClusterHistoryActivity() {
        return autoscaleClusterHistoryActivity;
    }

    public void setAutoscaleClusterHistoryActivity(AutoscaleClusterHistoryActivity autoscaleClusterHistoryActivity) {
        this.autoscaleClusterHistoryActivity = autoscaleClusterHistoryActivity;
    }
}
