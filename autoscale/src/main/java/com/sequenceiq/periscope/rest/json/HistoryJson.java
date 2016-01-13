package com.sequenceiq.periscope.rest.json;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.periscope.domain.AdjustmentType;
import com.sequenceiq.periscope.domain.AlertType;
import com.sequenceiq.periscope.domain.ScalingStatus;

public class HistoryJson implements Json {

    private long id;
    private long clusterId;
    private Long cbStackId;
    private int originalNodeCount;
    private int adjustment;
    private AdjustmentType adjustmentType;
    private ScalingStatus scalingStatus;
    private String statusReason;
    private long timestamp;
    private String hostGroup;
    private AlertType alertType;
    private Map<String, String> properties = new HashMap<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public Long getCbStackId() {
        return cbStackId;
    }

    public void setCbStackId(Long cbStackId) {
        this.cbStackId = cbStackId;
    }

    public int getOriginalNodeCount() {
        return originalNodeCount;
    }

    public void setOriginalNodeCount(int originalNodeCount) {
        this.originalNodeCount = originalNodeCount;
    }

    public int getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(int adjustment) {
        this.adjustment = adjustment;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
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

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
