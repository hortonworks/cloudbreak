package com.sequenceiq.periscope.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.periscope.api.endpoint.validator.ValidLoadAlertConfiguration;
import com.sequenceiq.periscope.doc.ApiDescription.LoadAlertJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@ValidLoadAlertConfiguration
public class LoadAlertConfigurationRequest implements Json {
    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MIN_RESOUCE_VALUE)
    @Min(value = 0)
    @Digits(fraction = 0, integer = 3)
    @NotNull
    private @Valid Integer minResourceValue;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MAX_RESOUCE_VALUE)
    @Min(value = 0)
    @Digits(fraction = 0, integer = 3)
    @NotNull
    private @Valid Integer maxResourceValue;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_COOL_DOWN_MINS_VALUE)
    @Min(value = 1)
    @Max(value = 180)
    @Digits(fraction = 0, integer = 3)
    private @Valid Integer coolDownMinutes;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_SCALE_UP_COOL_DOWN_MINS_VALUE)
    @Min(value = 1)
    @Max(value = 180)
    @Digits(fraction = 0, integer = 3)
    private @Valid Integer scaleUpCoolDownMinutes;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_SCALE_DOWN_COOL_DOWN_MINS_VALUE)
    @Min(value = 1)
    @Max(value = 180)
    @Digits(fraction = 0, integer = 3)
    private @Valid Integer scaleDownCoolDownMinutes;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MAX_SCALEUP_STEP_SIZE_VALUE)
    @Min(value = 2)
    @Max(value = 500)
    @Digits(fraction = 0, integer = 3)
    private @Valid Integer maxScaleUpStepSize;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION_MAX_SCALEDOWN_STEP_SIZE_VALUE)
    @Min(value = 2)
    @Max(value = 500)
    @Digits(fraction = 0, integer = 3)
    private @Valid Integer maxScaleDownStepSize;

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
