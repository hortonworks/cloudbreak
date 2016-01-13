package com.sequenceiq.periscope.rest.json;

import com.sequenceiq.periscope.monitor.evaluator.AlertState;

public class MetricAlertJson extends AbstractAlertJson {

    private String alertDefinition;
    private int period;
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
