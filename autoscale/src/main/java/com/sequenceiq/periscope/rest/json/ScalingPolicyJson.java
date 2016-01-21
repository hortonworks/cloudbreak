package com.sequenceiq.periscope.rest.json;

import javax.validation.constraints.Pattern;

import com.sequenceiq.periscope.domain.AdjustmentType;

public class ScalingPolicyJson implements Json {

    private Long id;
    @Pattern(regexp = "([a-zA-Z][-a-zA-Z0-9]*)",
            message = "The name can only contain alphanumeric characters and hyphens and has start with an alphanumeric character")
    private String name;
    private AdjustmentType adjustmentType;
    private int scalingAdjustment;
    private long alertId;

    private String hostGroup;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getAlertId() {
        return alertId;
    }

    public void setAlertId(long alertId) {
        this.alertId = alertId;
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
