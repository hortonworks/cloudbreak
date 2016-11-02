package com.sequenceiq.periscope.api.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.periscope.doc.ApiDescription.HistoryJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("History")
public class HistoryJson implements Json {

    @ApiModelProperty(HistoryJsonProperties.ID)
    private long id;

    @ApiModelProperty(HistoryJsonProperties.CLUSTERID)
    private long clusterId;

    @ApiModelProperty(HistoryJsonProperties.CBSTACKID)
    private Long cbStackId;

    @ApiModelProperty(HistoryJsonProperties.ORIGINALNODECOUNT)
    private int originalNodeCount;

    @ApiModelProperty(HistoryJsonProperties.ADJUSTMENT)
    private int adjustment;

    @ApiModelProperty(HistoryJsonProperties.ADJUSTMENTTYPE)
    private AdjustmentType adjustmentType;

    @ApiModelProperty(HistoryJsonProperties.SCALINGSTATUS)
    private ScalingStatus scalingStatus;

    @ApiModelProperty(HistoryJsonProperties.STATUSREASON)
    private String statusReason;

    @ApiModelProperty(HistoryJsonProperties.TIMESTAMP)
    private long timestamp;

    @ApiModelProperty(HistoryJsonProperties.HOSTGROUP)
    private String hostGroup;

    @ApiModelProperty(HistoryJsonProperties.ALERTTYPE)
    private AlertType alertType;

    @ApiModelProperty(HistoryJsonProperties.PROPERTIES)
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
