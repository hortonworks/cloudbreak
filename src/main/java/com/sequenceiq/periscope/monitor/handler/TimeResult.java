package com.sequenceiq.periscope.monitor.handler;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.TimeAlarm;

public class TimeResult extends BaseResult {

    private final TimeAlarm alarm;

    public TimeResult(boolean alarmHit, TimeAlarm alarm, Cluster cluster) {
        super(alarmHit, cluster);
        this.alarm = alarm;
    }

    public TimeAlarm getAlarm() {
        return alarm;
    }
}
