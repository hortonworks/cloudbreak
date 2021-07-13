package com.sequenceiq.periscope.domain;

import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS;
import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_MAX_SCALE_DOWN_STEP_SIZE;
import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_MAX_SCALE_UP_STEP_SIZE;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LoadAlertConfiguration {

    private Integer minResourceValue;

    private Integer maxResourceValue;

    private Integer coolDownMinutes = DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS;

    private Integer scaleUpCoolDownMinutes;

    private Integer scaleDownCoolDownMinutes;

    private Integer maxScaleUpStepSize = DEFAULT_MAX_SCALE_UP_STEP_SIZE;

    private Integer maxScaleDownStepSize = DEFAULT_MAX_SCALE_DOWN_STEP_SIZE;

    public Integer getMinResourceValue() {
        return minResourceValue;
    }

    public void setMinResourceValue(Integer minResourceValue) {
        this.minResourceValue = minResourceValue;
    }

    public Integer getMaxResourceValue() {
        return maxResourceValue;
    }

    public void setMaxResourceValue(Integer maxResourceValue) {
        this.maxResourceValue = maxResourceValue;
    }

    public Integer getCoolDownMinutes() {
        return coolDownMinutes;
    }

    @JsonIgnore
    public Long getPollingCoolDownMillis() {
        return Math.min(getScaleUpCoolDownMillis(), getScaleDownCoolDownMillis());
    }

    @JsonIgnore
    public Long getScaleUpCoolDownMillis() {
        Integer scaleUpCoolDown = scaleUpCoolDownMinutes != null ? scaleUpCoolDownMinutes : coolDownMinutes;
        return TimeUnit.MILLISECONDS.convert(scaleUpCoolDown, TimeUnit.MINUTES);
    }

    @JsonIgnore
    public Long getScaleDownCoolDownMillis() {
        Integer scaleDownCoolDown = scaleDownCoolDownMinutes != null ? scaleDownCoolDownMinutes : coolDownMinutes;
        return TimeUnit.MILLISECONDS.convert(scaleDownCoolDown, TimeUnit.MINUTES);
    }

    public void setCoolDownMinutes(Integer coolDownMinutes) {
        if (null != coolDownMinutes) {
            this.coolDownMinutes = coolDownMinutes;
        }
    }

    public void setScaleUpCoolDownMinutes(Integer scaleUpCoolDownMinutes) {
        if (null != scaleUpCoolDownMinutes) {
            this.scaleUpCoolDownMinutes = scaleUpCoolDownMinutes;
        }
    }

    public void setScaleDownCoolDownMinutes(Integer scaleDownCoolDownMinutes) {
        if (null != scaleDownCoolDownMinutes) {
            this.scaleDownCoolDownMinutes = scaleDownCoolDownMinutes;
        }
    }

    public Integer getScaleUpCoolDownMinutes() {
        return scaleUpCoolDownMinutes;
    }

    public Integer getScaleDownCoolDownMinutes() {
        return scaleDownCoolDownMinutes;
    }

    public Integer getMaxScaleUpStepSize() {
        return maxScaleUpStepSize;
    }

    public void setMaxScaleUpStepSize(Integer maxScaleUpStepSize) {
        if (null != maxScaleUpStepSize) {
            this.maxScaleUpStepSize = maxScaleUpStepSize;
        }
    }

    public Integer getMaxScaleDownStepSize() {
        return maxScaleDownStepSize;
    }

    public void setMaxScaleDownStepSize(Integer maxScaleDownStepSize) {
        if (null != maxScaleDownStepSize) {
            this.maxScaleDownStepSize = maxScaleDownStepSize;
        }
    }

    @Override
    public String toString() {
        return "LoadAlertConfiguration{" +
                "minResourceValue=" + minResourceValue +
                ", maxResourceValue=" + maxResourceValue +
                ", coolDownMinutes=" + coolDownMinutes +
                ", scaleUpCoolDownMinutes=" + scaleUpCoolDownMinutes +
                ", scaleDownCoolDownMinutes=" + scaleDownCoolDownMinutes +
                ", maxScaleDownStepSize=" + maxScaleDownStepSize +
                ", maxScaleUpStepSize=" + maxScaleUpStepSize +
                '}';
    }
}
