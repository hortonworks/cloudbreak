package com.sequenceiq.periscope.domain;

import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LoadAlertConfiguration {

    private Integer minResourceValue;

    private Integer maxResourceValue;

    private Integer coolDownMinutes = DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS;

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
    public Long getCoolDownMillis() {
        return TimeUnit.MILLISECONDS.convert(coolDownMinutes, TimeUnit.MINUTES);
    }

    public void setCoolDownMinutes(Integer coolDownMinutes) {
        if (null != coolDownMinutes) {
            this.coolDownMinutes = coolDownMinutes;
        }
    }

    @Override
    public String toString() {
        return "LoadAlertConfiguration{" +
                "minResourceValue=" + minResourceValue +
                ", maxResourceValue=" + maxResourceValue +
                ", coolDownMinutes=" + coolDownMinutes +
                '}';
    }
}
