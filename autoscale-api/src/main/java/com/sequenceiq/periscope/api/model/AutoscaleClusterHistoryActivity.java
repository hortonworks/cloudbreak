package com.sequenceiq.periscope.api.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.periscope.doc.ApiDescription;
import io.swagger.annotations.ApiModelProperty;

public class AutoscaleClusterHistoryActivity {

    @ApiModelProperty(ApiDescription.HistoryJsonProperties.ALERTTYPE)
    @JsonProperty("autoscalingType")
    private AlertType alertType;

    @ApiModelProperty(ApiDescription.HistoryJsonProperties.ADJUSTMENTTYPE)
    private AdjustmentType adjustmentType;

    @ApiModelProperty(ApiDescription.HistoryJsonProperties.ADJUSTMENT)
    private int adjustment;

    @ApiModelProperty(ApiDescription.HistoryJsonProperties.HOSTGROUP)
    private String hostGroup;

    @ApiModelProperty(ApiDescription.HistoryJsonProperties.ORIGINALNODECOUNT)
    private int originalNodeCount;

    @ApiModelProperty(ApiDescription.HistoryJsonProperties.PROPERTIES)
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
