package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.LoadAlertJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class LoadAlertConfigurationResponse implements Json {

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MIN_RESOUCE_VALUE)
    private Integer minResourceValue;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MAX_RESOUCE_VALUE)
    private Integer maxResourceValue;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_COOL_DOWN_MINS_VALUE)
    private Integer coolDownMinutes;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_SCALE_UP_COOL_DOWN_MINS_VALUE)
    private Integer scaleUpCoolDownMinutes;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_SCALE_DOWN_COOL_DOWN_MINS_VALUE)
    private Integer scaleDownCoolDownMinutes;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MAX_SCALEUP_STEP_SIZE_VALUE)
    private Integer maxScaleUpStepSize;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MAX_SCALEDOWN_STEP_SIZE_VALUE)
    private Integer maxScaleDownStepSize;

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

    public Integer getScaleUpCoolDownMinutes() {
        return scaleUpCoolDownMinutes;
    }

    public void setScaleUpCoolDownMinutes(Integer scaleUpCoolDownMinutes) {
        this.scaleUpCoolDownMinutes = scaleUpCoolDownMinutes;
    }

    public Integer getScaleDownCoolDownMinutes() {
        return scaleDownCoolDownMinutes;
    }

    public void setScaleDownCoolDownMinutes(Integer scaleDownCoolDownMinutes) {
        this.scaleDownCoolDownMinutes = scaleDownCoolDownMinutes;
    }

    public Integer getMaxScaleUpStepSize() {
        return maxScaleUpStepSize;
    }

    public void setMaxScaleUpStepSize(Integer maxScaleUpStepSize) {
        this.maxScaleUpStepSize = maxScaleUpStepSize;
    }

    public Integer getMaxScaleDownStepSize() {
        return maxScaleDownStepSize;
    }

    public void setMaxScaleDownStepSize(Integer maxScaleDownStepSize) {
        this.maxScaleDownStepSize = maxScaleDownStepSize;
    }
}
