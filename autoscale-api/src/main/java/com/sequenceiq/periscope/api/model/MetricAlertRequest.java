package com.sequenceiq.periscope.api.model;

import javax.validation.Valid;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;
import com.sequenceiq.periscope.doc.ApiDescription.MetricAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class MetricAlertRequest extends AbstractAlertJson {

    @ApiModelProperty(MetricAlertJsonProperties.ALERTDEFINITION)
    private String alertDefinition;

    @ApiModelProperty(MetricAlertJsonProperties.PERIOD)
    private int period;

    @ApiModelProperty(MetricAlertJsonProperties.ALERTSTATE)
    private AlertState alertState;

    @Valid
    @ApiModelProperty(BaseAlertJsonProperties.SCALINGPOLICYID)
    private ScalingPolicyRequest scalingPolicy;

    public String getAlertDefinition() {
        return alertDefinition;
    }

    public void setAlertDefinition(String alertDefinition) {
        this.alertDefinition = alertDefinition;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public AlertState getAlertState() {
        return alertState;
    }

    public void setAlertState(AlertState alertState) {
        this.alertState = alertState;
    }

    public ScalingPolicyRequest getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicyRequest scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }
}
