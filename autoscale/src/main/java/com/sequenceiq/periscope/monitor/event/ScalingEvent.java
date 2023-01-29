package com.sequenceiq.periscope.monitor.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.model.ScalingAdjustmentType;

public class ScalingEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1587121489597L;

    private List<String> decommissionNodeIds;

    private transient Integer existingHostGroupNodeCount;

    private transient Integer existingClusterNodeCount;

    private transient Integer desiredAbsoluteHostGroupNodeCount;

    private transient Integer existingServiceHealthyHostGroupNodeCount;

    private transient Long activityId;

    private transient ScalingAdjustmentType scalingAdjustmentType;

    public ScalingEvent(BaseAlert alert) {
        super(alert);
    }

    public BaseAlert getAlert() {
        return (BaseAlert) getSource();
    }

    public List<String> getDecommissionNodeIds() {
        return decommissionNodeIds != null ? decommissionNodeIds : List.of();
    }

    public void setDecommissionNodeIds(List<String> decommissionNodeIds) {
        this.decommissionNodeIds = decommissionNodeIds;
    }

    public Integer getExistingHostGroupNodeCount() {
        return existingHostGroupNodeCount;
    }

    public void setExistingHostGroupNodeCount(Integer existingHostGroupNodeCount) {
        this.existingHostGroupNodeCount = existingHostGroupNodeCount;
    }

    public Integer getDesiredAbsoluteHostGroupNodeCount() {
        return desiredAbsoluteHostGroupNodeCount;
    }

    public void setDesiredAbsoluteHostGroupNodeCount(Integer desiredAbsoluteHostGroupNodeCount) {
        this.desiredAbsoluteHostGroupNodeCount = desiredAbsoluteHostGroupNodeCount;
    }

    public Integer getExistingClusterNodeCount() {
        return existingClusterNodeCount;
    }

    public void setExistingClusterNodeCount(Integer existingClusterNodeCount) {
        this.existingClusterNodeCount = existingClusterNodeCount;
    }

    public Integer getExistingServiceHealthyHostGroupNodeCount() {
        return existingServiceHealthyHostGroupNodeCount;
    }

    public void setExistingServiceHealthyHostGroupNodeCount(Integer existingServiceHealthyHostGroupNodeCount) {
        this.existingServiceHealthyHostGroupNodeCount = existingServiceHealthyHostGroupNodeCount;
    }

    public ScalingAdjustmentType getScalingAdjustmentType() {
        return scalingAdjustmentType;
    }

    public void setScalingAdjustmentType(ScalingAdjustmentType scalingAdjustmentType) {
        this.scalingAdjustmentType = scalingAdjustmentType;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }
}
