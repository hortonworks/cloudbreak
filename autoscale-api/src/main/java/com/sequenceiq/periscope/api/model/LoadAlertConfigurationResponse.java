package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.LoadAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LoadAlertConfigurationResponse implements Json {

    @ApiModelProperty(LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MIN_RESOUCE_VALUE)
    private Integer minResourceValue;

    @ApiModelProperty(LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MAX_RESOUCE_VALUE)
    private Integer maxResourceValue;

    @ApiModelProperty(LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_COOL_DOWN_MINS_VALUE)
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

    public void setCoolDownMinutes(Integer coolDownMinutes) {
        this.coolDownMinutes = coolDownMinutes;
    }
}
