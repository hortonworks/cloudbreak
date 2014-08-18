package com.sequenceiq.periscope.rest.json;

import java.util.List;

public class AlarmsJson implements Json {

    private List<AlarmJson> alarms;

    public AlarmsJson() {
    }

    public AlarmsJson(List<AlarmJson> alarms) {
        this.alarms = alarms;
    }

    public List<AlarmJson> getAlarms() {
        return alarms;
    }

    public void setAlarms(List<AlarmJson> alarms) {
        this.alarms = alarms;
    }

}
