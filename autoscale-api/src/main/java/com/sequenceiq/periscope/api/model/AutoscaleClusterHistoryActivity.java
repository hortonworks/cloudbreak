package com.sequenceiq.periscope.api.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.v3.oas.annotations.media.Schema;

public class AutoscaleClusterHistoryActivity {

    @Schema(description = ApiDescription.HistoryJsonProperties.ALERTTYPE)
    @JsonProperty("autoscalingType")
    private AlertType alertType;

    @Schema(description = ApiDescription.HistoryJsonProperties.ADJUSTMENTTYPE)
    private AdjustmentType adjustmentType;

    @Schema(description = ApiDescription.HistoryJsonProperties.ADJUSTMENT, requiredMode = Schema.RequiredMode.REQUIRED)
    private int adjustment;

    @Schema(description = ApiDescription.HistoryJsonProperties.HOSTGROUP)
    private String hostGroup;

    @Schema(description = ApiDescription.HistoryJsonProperties.ORIGINALNODECOUNT, requiredMode = Schema.RequiredMode.REQUIRED)
    private int originalNodeCount;

    @Schema(description = ApiDescription.HistoryJsonProperties.PROPERTIES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> properties = new HashMap<>();

    public String getAlertType() {
        return Optional.ofNullable(alertType).map(AlertType::toString).orElse("");
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public int getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(int adjustment) {
        this.adjustment = adjustment;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public int getOriginalNodeCount() {
        return originalNodeCount;
    }

    public void setOriginalNodeCount(int originalNodeCount) {
        this.originalNodeCount = originalNodeCount;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
