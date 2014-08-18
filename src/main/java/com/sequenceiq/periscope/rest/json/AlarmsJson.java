package com.sequenceiq.periscope.rest.json;

import java.util.List;

public class AlarmsJson implements Json {

    private List<AlarmJson> alarms;

    public List<AlarmJson> getAlarms() {
        return alarms;
    }

    public void setAlarms(List<AlarmJson> alarms) {
        this.alarms = alarms;
    }

}
