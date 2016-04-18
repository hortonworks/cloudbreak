package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.MetricAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("MetricAlertJson")
public class MetricAlertJson extends AbstractAlertJson {

    @ApiModelProperty(MetricAlertJsonProperties.ALERTDEFINITION)
    private String alertDefinition;
    @ApiModelProperty(MetricAlertJsonProperties.PERIOD)
    private int period;
    @ApiModelProperty(MetricAlertJsonProperties.ALERTSTATE)
    private AlertState alertState;

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
}
