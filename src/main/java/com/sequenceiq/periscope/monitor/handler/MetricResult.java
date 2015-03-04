package com.sequenceiq.periscope.monitor.handler;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlarm;

public class MetricResult extends AbstractResult {

    private final MetricAlarm alarm;

    public MetricResult(boolean alarmHit, MetricAlarm alarm, Cluster cluster) {
        super(alarmHit, cluster);
        this.alarm = alarm;
    }

    public MetricAlarm getAlarm() {
        return alarm;
    }

}
