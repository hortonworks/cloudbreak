package com.sequenceiq.periscope.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.converter.db.ActivityStatusAttributeConverter;

@Entity
@Table(name = "scaling_activity")
public class ScalingActivity implements Clustered {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "scaling_activity_generator")
    @SequenceGenerator(name  = "scaling_activity_generator", sequenceName = "scaling_activity_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "activity_crn")
    private String activityCrn;

    @Column(name = "flow_id")
    private String flowId;

    @Column(name = "start_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = "end_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @Column(name = "activity_reason")
    private String scalingActivityReason;

    @Column(name = "activity_status")
    @Convert(converter = ActivityStatusAttributeConverter.class)
    private ActivityStatus activityStatus = ActivityStatus.ACTIVITY_PENDING;

    @ManyToOne
    private Cluster cluster;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActivityCrn() {
        return activityCrn;
    }

    public void setActivityCrn(String activityCrn) {
        this.activityCrn = activityCrn;
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

    public String getScalingActivityReason() {
        return scalingActivityReason;
    }

    public void setScalingActivityReason(String scalingActivityReason) {
        this.scalingActivityReason = scalingActivityReason;
    }

    public ActivityStatus getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(ActivityStatus activityStatus) {
        this.activityStatus = activityStatus;
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
        return "ScalingActivity{" +
                "id=" + id +
                ", activityCrn='" + activityCrn + '\'' +
                ", flowId='" + flowId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", scalingActivityReason='" + scalingActivityReason + '\'' +
                ", activityStatus=" + activityStatus +
                ", cluster=" + cluster +
                '}';
    }
}
