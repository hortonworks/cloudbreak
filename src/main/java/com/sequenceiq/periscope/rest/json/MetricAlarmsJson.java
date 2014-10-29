package com.sequenceiq.periscope.rest.json;

import java.util.List;

public class MetricAlarmsJson implements Json {

    private List<MetricAlarmJson> alarms;

    public MetricAlarmsJson() {
    }

    public MetricAlarmsJson(List<MetricAlarmJson> alarms) {
        this.alarms = alarms;
    }

    public List<MetricAlarmJson> getAlarms() {
        return alarms;
    }

    public void setAlarms(List<MetricAlarmJson> alarms) {
        this.alarms = alarms;
    }

}
