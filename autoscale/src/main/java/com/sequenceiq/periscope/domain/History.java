package com.sequenceiq.periscope.domain;

import static com.sequenceiq.periscope.common.AlertConstants.ALERT_NAME;
import static com.sequenceiq.periscope.common.AlertConstants.SCALING_TARGET;
import static com.sequenceiq.periscope.common.AlertConstants.TIME_ZONE;
import static com.sequenceiq.periscope.common.AlertConstants.CRON;
import static com.sequenceiq.periscope.common.AlertConstants.PARAMETERS;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.converter.AdjustmentTypeConverter;
import com.sequenceiq.periscope.converter.AlertTypeConverter;
import com.sequenceiq.periscope.converter.ScalingStatusConverter;

@Entity
@NamedQueries({
        @NamedQuery(name = "History.findAllByCluster", query = "SELECT c FROM History c WHERE c.clusterId= :id"),
        @NamedQuery(name = "History.findByCluster", query = "SELECT c FROM History c WHERE c.clusterId= :clusterId AND c.id= :historyId")
})
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "history_generator")
    @SequenceGenerator(name = "history_generator", sequenceName = "history_id_seq", allocationSize = 1)
    private long id;

    @Column(name = "cluster_id")
    private long clusterId;

    @Column(name = "cb_stack_crn")
    private String stackCrn;

    @Column(name = "original_node_count")
    private int originalNodeCount;

    private int adjustment;

    @Column(name = "adjustment_type")
    @Convert(converter = AdjustmentTypeConverter.class)
    private AdjustmentType adjustmentType;

    @Column(name = "status")
    @Convert(converter = ScalingStatusConverter.class)
    private ScalingStatus scalingStatus;

    @Column(name = "status_reason")
    private String statusReason;

    private long timestamp;

    @Column(name = "host_group")
    private String hostGroup;

    @Column(name = "alert_type")
    @Convert(converter = AlertTypeConverter.class)
    private AlertType alertType;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value", columnDefinition = "TEXT", length = 100000)
    private Map<String, String> properties = new HashMap<>();

    public History() {
    }

    public History(ScalingStatus status, String statusReason, String stackCrn) {
        this.scalingStatus = status;
        this.statusReason = statusReason;
        this.stackCrn = stackCrn;
        this.timestamp = System.currentTimeMillis();
    }

    public History(ScalingStatus status, String statusReason, int originalNodeCount, int adjustment) {
        this.scalingStatus = status;
        this.statusReason = statusReason;
        this.originalNodeCount = originalNodeCount;
        this.adjustment = adjustment;
        this.timestamp = System.currentTimeMillis();
    }

    public History withScalingPolicy(ScalingPolicy policy) {
        this.adjustmentType = policy.getAdjustmentType();
        this.hostGroup = policy.getHostGroup();
        return this;
    }

    public History withAlert(BaseAlert alert) {
        if (alert instanceof TimeAlert) {
            TimeAlert ta = (TimeAlert) alert;
            properties.put(TIME_ZONE, ta.getTimeZone());
            properties.put(CRON, ta.getCron());
            properties.put(SCALING_TARGET, "" + ta.getScalingPolicy().getScalingAdjustment());
            alertType = AlertType.TIME;
        } else if (alert instanceof  LoadAlert) {
            LoadAlert la = (LoadAlert) alert;
            properties.put(PARAMETERS, la.getLoadAlertConfiguration().toString());
            alertType = AlertType.LOAD;
        }
        properties.put(ALERT_NAME, alert.getName());
        return this;
    }

    public History withCluster(Cluster cluster) {
        clusterId = cluster.getId();
        stackCrn = cluster.getStackCrn();
        return this;
    }

    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }
}
