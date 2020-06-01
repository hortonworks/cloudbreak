package com.sequenceiq.periscope.api.model;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.sequenceiq.periscope.api.endpoint.validator.ValidLoadAlertConfiguration;
import com.sequenceiq.periscope.doc.ApiDescription.LoadAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidLoadAlertConfiguration
public class LoadAlertConfigurationRequest implements Json {
    @ApiModelProperty(LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MIN_RESOUCE_VALUE)
    @Min(value = 0)
    @Max(value = 200)
    @Digits(fraction = 0, integer = 3)
    @NotNull
    private @Valid Integer minResourceValue;

    @ApiModelProperty(LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MAX_RESOUCE_VALUE)
    @Min(value = 0)
    @Max(value = 200)
    @Digits(fraction = 0, integer = 3)
    @NotNull
    private @Valid Integer maxResourceValue;

    @ApiModelProperty(LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_COOL_DOWN_MINS_VALUE)
    @Min(value = 2)
    @Max(value = 180)
    @Digits(fraction = 0, integer = 3)
    @NotNull
    private @Valid Integer coolDownMinutes;

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
