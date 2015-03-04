package com.sequenceiq.periscope.monitor.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.TimeAlarm;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.monitor.MonitorUpdateRate;
import com.sequenceiq.periscope.monitor.event.EventType;
import com.sequenceiq.periscope.monitor.event.TimeUpdateEvent;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.DateUtils;

@Component
public class CronTimeHandler implements TimeHandler {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(CronTimeHandler.class);

    @Autowired
    private ClusterService clusterService;

    @Override
    public List<TimeResult> isTrigger(ApplicationEvent event) {
        TimeUpdateEvent source = (TimeUpdateEvent) event;
        long clusterId = source.getClusterId();
        try {
            Cluster cluster = clusterService.get(clusterId);
            return checkTimeAlarms(cluster);
        } catch (ClusterNotFoundException e) {
            LOGGER.error(clusterId, "Cluster not found, ignoring event", e);
        }
        return Collections.emptyList();
    }

    private List<TimeResult> checkTimeAlarms(Cluster cluster) {
        List<TimeResult> result = new ArrayList<>();
        for (TimeAlarm alarm : cluster.getTimeAlarms()) {
            long clusterId = cluster.getId();
            String alarmName = alarm.getName();
            LOGGER.debug(clusterId, "Checking time based alarm: '{}'", alarmName);
            if (isTrigger(alarm, clusterId)) {
                LOGGER.info(clusterId, "Alarm: '{}' triggers", alarmName);
                result.add(new TimeResult(true, alarm, cluster));
            } else {
                result.add(new TimeResult(false, alarm, cluster));
            }
        }
        return result;
    }

    private boolean isTrigger(TimeAlarm alarm, long clusterId) {
        return DateUtils.isTrigger(clusterId, alarm.getCron(), alarm.getTimeZone(), MonitorUpdateRate.CLUSTER_UPDATE_RATE);
    }

    @Override
    public EventType getEventType() {
        return EventType.TIME;
    }
}
