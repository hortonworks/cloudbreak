package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;
import com.sequenceiq.periscope.doc.ApiDescription.MetricAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("MetricAlertResponse")
public class MetricAlertResponse extends AbstractAlertJson {

    @ApiModelProperty(BaseAlertJsonProperties.ID)
    private Long id;

    @ApiModelProperty(MetricAlertJsonProperties.ALERTDEFINITION)
    private String alertDefinition;

    @ApiModelProperty(MetricAlertJsonProperties.ALERTDEFINITION_LABEL)
    private String alertDefinitionLabel;

    @ApiModelProperty(MetricAlertJsonProperties.PERIOD)
    private int period;

    @ApiModelProperty(MetricAlertJsonProperties.ALERTSTATE)
    private AlertState alertState;

    @ApiModelProperty(BaseAlertJsonProperties.SCALINGPOLICYID)
    private Long scalingPolicyId;

    @ApiModelProperty(BaseAlertJsonProperties.SCALINGPOLICYID)
    private ScalingPolicyRequest scalingPolicy;

    public String getAlertDefinition() {
        return alertDefinition;
    }

    public void setAlertDefinition(String alertDefinition) {
        this.alertDefinition = alertDefinition;
    }

    public String getAlertDefinitionLabel() {
        return alertDefinitionLabel;
    }

    public void setAlertDefinitionLabel(String alertDefinitionLabel) {
        this.alertDefinitionLabel = alertDefinitionLabel;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScalingPolicyId() {
        return scalingPolicyId;
    }

    public void setScalingPolicyId(Long scalingPolicyId) {
        this.scalingPolicyId = scalingPolicyId;
    }

    public ScalingPolicyRequest getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicyRequest scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }
}
