package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.PrometheusAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("PromhetheusAlert")
public class PrometheusAlertJson extends AbstractAlertJson {
    private static final int DEFAULT_PERIOD = 1;

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

    @Override
    public String toString() {
        return "PrometheusAlertJson{"
                + "alertRuleName='" + alertRuleName
                + "', period=" + period
                + ", threshold=" + threshold
                + ", alertState=" + alertState
                + ", alertOperator=" + alertOperator
                + '}';
    }
}
