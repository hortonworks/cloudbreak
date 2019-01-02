package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;
import com.sequenceiq.periscope.doc.ApiDescription.PrometheusAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class PrometheusAlertResponse extends AbstractAlertJson {
    private static final int DEFAULT_PERIOD = 1;

    @ApiModelProperty(BaseAlertJsonProperties.ID)
    private Long id;

    @ApiModelProperty(PrometheusAlertJsonProperties.ALERTRULE)
    private String alertRuleName;

    @ApiModelProperty(PrometheusAlertJsonProperties.PERIOD)
    private int period = DEFAULT_PERIOD;

    @ApiModelProperty(PrometheusAlertJsonProperties.THRESHOLD)
    private double threshold;

    @ApiModelProperty(PrometheusAlertJsonProperties.ALERTSTATE)
    private AlertState alertState;

    @ApiModelProperty(PrometheusAlertJsonProperties.ALERTOPERATOR)
    private AlertOperator alertOperator;

    @ApiModelProperty(BaseAlertJsonProperties.SCALINGPOLICYID)
    private Long scalingPolicyId;

    @ApiModelProperty(BaseAlertJsonProperties.SCALINGPOLICYID)
    private ScalingPolicyRequest scalingPolicy;

    public String getAlertRuleName() {
        return alertRuleName;
    }

    public void setAlertRuleName(String alertRuleName) {
        this.alertRuleName = alertRuleName;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public AlertState getAlertState() {
        return alertState;
    }

    public void setAlertState(AlertState alertState) {
        this.alertState = alertState;
    }

    public AlertOperator getAlertOperator() {
        return alertOperator;
    }

    public void setAlertOperator(AlertOperator alertOperator) {
        this.alertOperator = alertOperator;
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

    @Override
    public String toString() {
        return "PrometheusAlertRequest{"
                + "alertRuleName='" + alertRuleName
                + "', period=" + period
                + ", threshold=" + threshold
                + ", alertState=" + alertState
                + ", alertOperator=" + alertOperator
                + '}';
    }
}
