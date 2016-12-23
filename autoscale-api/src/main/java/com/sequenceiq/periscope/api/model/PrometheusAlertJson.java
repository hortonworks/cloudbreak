package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("PromhetheusAlert")
public class PrometheusAlertJson extends AbstractAlertJson {

    @ApiModelProperty(ApiDescription.PrometheusAlertJsonProperties.ALERTRULE)
    private String alertRule;

    @ApiModelProperty(ApiDescription.PrometheusAlertJsonProperties.PERIOD)
    private int period;

    @ApiModelProperty(ApiDescription.PrometheusAlertJsonProperties.ALERTSTATE)
    private AlertState alertState;

    public String getAlertRule() {
        return alertRule;
    }

    public void setAlertRule(String alertRule) {
        this.alertRule = alertRule;
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
}
