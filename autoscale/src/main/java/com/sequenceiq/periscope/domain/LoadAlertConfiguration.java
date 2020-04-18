package com.sequenceiq.periscope.domain;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LoadAlertConfiguration {

    private Integer minResourceValue;

    private Integer maxResourceValue;

    private Integer coolDownMinutes;

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
        this.coolDownMinutes = coolDownMinutes;
    }
}
