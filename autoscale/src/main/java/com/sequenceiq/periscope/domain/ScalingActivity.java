package com.sequenceiq.periscope.domain;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.converter.db.ActivityStatusAttributeConverter;

@Entity
@Table(name = "scaling_activity")
public class ScalingActivity implements Clustered {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "scaling_activity_generator")
    @SequenceGenerator(name  = "scaling_activity_generator", sequenceName = "scaling_activity_id_seq", allocationSize = 1)
    private Long id;

    @Deprecated
    @Column(name = "activity_crn")
    private String activityCrn;

    @Column(name = "operation_id")
    private String operationId;

    @Column(name = "flow_id")
    private String flowId;

    @Column(name = "yarn_recommendation_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date yarnRecommendationTime;

    @Column(name = "start_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = "end_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @Column(name = "activity_reason")
    private String scalingActivityReason;

    @Column(name = "yarn_recommendation")
    private String yarnRecommendation;

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

    @Deprecated
    public String getActivityCrn() {
        return activityCrn;
    }

    @Deprecated
    public void setActivityCrn(String activityCrn) {
        this.activityCrn = activityCrn;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public Date getYarnRecommendationTime() {
        return yarnRecommendationTime;
    }

    public void setYarnRecommendationTime(Date yarnRecommendationTime) {
        this.yarnRecommendationTime = yarnRecommendationTime;
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

    public String getYarnRecommendation() {
        return yarnRecommendation;
    }

    public void setYarnRecommendation(String yarnRecommendation) {
        this.yarnRecommendation = yarnRecommendation;
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
                ", operationId='" + operationId + '\'' +
                ", flowId='" + flowId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", yarnRecommendationTime=" + yarnRecommendationTime +
                ", yarnRecommendation='" + yarnRecommendation + '\'' +
                ", scalingActivityReason='" + scalingActivityReason + '\'' +
                ", activityStatus=" + activityStatus +
                ", cluster=" + cluster +
                '}';
    }
}
