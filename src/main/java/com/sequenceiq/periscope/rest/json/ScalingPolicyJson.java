package com.sequenceiq.periscope.rest.json;

import com.sequenceiq.periscope.domain.AdjustmentType;

public class ScalingPolicyJson implements Json {

    private Long id;
    private String name;
    private AdjustmentType adjustmentType;
    private int scalingAdjustment;
    private long alarmId;
    private String hostGroup;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(long alarmId) {
        this.alarmId = alarmId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public int getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(int scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }
}
