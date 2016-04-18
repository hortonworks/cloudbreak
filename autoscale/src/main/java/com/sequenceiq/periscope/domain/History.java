package com.sequenceiq.periscope.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

@Entity
@NamedQueries({
        @NamedQuery(name = "History.findAllByCluster", query = "SELECT c FROM History c WHERE c.clusterId= :id"),
        @NamedQuery(name = "History.findByCluster", query = "SELECT c FROM History c WHERE c.clusterId= :clusterId AND c.id= :historyId")
})
public class History {

    public static final String ALERT_DEFINITION = "alertDefinition";
    public static final String PERIOD = "period";
    public static final String ALERT_STATE = "alertState";
    public static final String ALERT_DESCRIPTION = "alertDescription";
    public static final String TIME_ZONE = "timeZone";
    public static final String CRON = "cron";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "history_generator")
    @SequenceGenerator(name = "history_generator", sequenceName = "sequence_table")
    private long id;

    @Column(name = "cluster_id")
    private long clusterId;

    @Column(name = "cb_stack_id")
    private Long cbStackId;

    @Column(name = "original_node_count")
    private int originalNodeCount;

    private int adjustment;

    @Column(name = "adjustment_type")
    @Enumerated(EnumType.STRING)
    private AdjustmentType adjustmentType;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ScalingStatus scalingStatus;

    @Column(name = "status_reason")
    private String statusReason;

    private long timestamp;

    @Column(name = "host_group")
    private String hostGroup;

    @Column(name = "alert_type")
    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value", columnDefinition = "TEXT", length = 100000)
    private Map<String, String> properties = new HashMap<>();

    public History() {
    }

    public History(ScalingStatus status, String statusReason, int originalNodeCount) {
        this.scalingStatus = status;
        this.statusReason = statusReason;
        this.originalNodeCount = originalNodeCount;
        this.timestamp = System.currentTimeMillis();
    }

    public History withScalingPolicy(ScalingPolicy policy) {
        this.adjustment = policy.getScalingAdjustment();
        this.adjustmentType = policy.getAdjustmentType();
        this.hostGroup = policy.getHostGroup();
        return this;
    }

    public History withAlert(BaseAlert alert) {
        if (alert instanceof MetricAlert) {
            MetricAlert ma = (MetricAlert) alert;
            this.properties.put(ALERT_DEFINITION, ma.getDefinitionName());
            this.properties.put(PERIOD, "" + ma.getPeriod());
            this.properties.put(ALERT_STATE, ma.getAlertState().name());
            this.alertType = AlertType.METRIC;
        } else if (alert instanceof TimeAlert) {
            TimeAlert ta = (TimeAlert) alert;
            this.properties.put(TIME_ZONE, ta.getTimeZone());
            this.properties.put(CRON, ta.getCron());
            this.alertType = AlertType.TIME;
        }
        this.properties.put(ALERT_DESCRIPTION, alert.getDescription());
        return this;
    }

    public History withCluster(Cluster cluster) {
        this.clusterId = cluster.getId();
        this.cbStackId = cluster.getStackId();
        this.userId = cluster.getUser().getId();
        return this;
    }

    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
