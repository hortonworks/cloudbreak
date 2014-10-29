package com.sequenceiq.periscope.rest.json;

import java.util.List;

public class TimeAlarmsJson implements Json {

    private List<TimeAlarmJson> alarms;

    public TimeAlarmsJson() {
    }

    public TimeAlarmsJson(List<TimeAlarmJson> alarms) {
        this.alarms = alarms;
    }

    public List<TimeAlarmJson> getAlarms() {
        return alarms;
    }

    public void setAlarms(List<TimeAlarmJson> alarms) {
        this.alarms = alarms;
    }

}
