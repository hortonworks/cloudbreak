package com.sequenceiq.periscope.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.sequenceiq.periscope.api.model.TriggerStatus;

@Entity
@Table(name = "scaling_trigger")
public class ScalingTrigger implements Clustered {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "scaling_trigger_generator")
    @SequenceGenerator(name  = "scaling_trigger_generator", sequenceName = "scaling_trigger_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "trigger_crn")
    private String triggerCrn;

    @Column(name = "flow_id")
    private String flowId;

    @Column(name = "start_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = "end_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @Column(name = "trigger_reason")
    private String triggerReason;

    @Column(name = "trigger_status")
    @Enumerated(EnumType.STRING)
    private TriggerStatus triggerStatus;

    @ManyToOne
    private Cluster cluster;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTriggerCrn() {
        return triggerCrn;
    }

    public void setTriggerCrn(String triggerCrn) {
        this.triggerCrn = triggerCrn;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(String triggerReason) {
        this.triggerReason = triggerReason;
    }

    public TriggerStatus getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(TriggerStatus triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    @Override
    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public String toString() {
        return "ScalingTrigger{" +
                "id=" + id +
                ", triggerCrn='" + triggerCrn + '\'' +
                ", flowId='" + flowId + '\'' +
                ", startTime=" + startTime +
                ", yarnMetrics='" + triggerReason + '\'' +
                ", triggerStatus=" + triggerStatus +
                ", cluster=" + cluster +
                '}';
    }
}
