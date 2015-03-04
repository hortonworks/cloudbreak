package com.sequenceiq.periscope.monitor.handler;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.BaseAlarm;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlarm;
import com.sequenceiq.periscope.domain.Notification;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlarm;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.monitor.event.EventType;
import com.sequenceiq.periscope.monitor.event.EventWrapper;
import com.sequenceiq.periscope.monitor.event.TypedEvent;
import com.sequenceiq.periscope.service.NotificationService;
import com.sequenceiq.periscope.service.ScalingService;

@Component
public class ClusterWatcher implements ApplicationListener<EventWrapper> {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ClusterWatcher.class);

    @Resource(name = "metricHandlers")
    private Map<EventType, MetricHandler> metricHandlers;
    @Autowired
    private TimeHandler timeHandler;
    @Autowired
    private ScalingService scalingService;
    @Autowired
    private NotificationService notificationService;

    @Override
    public void onApplicationEvent(EventWrapper event) {
        TypedEvent typedEvent = event.getSource();
        EventType eventType = typedEvent.getEventType();

        if (EventType.TIME.equals(eventType)) {
            List<TimeResult> timeHit = timeHandler.isTrigger((ApplicationEvent) typedEvent);
            for (TimeResult result : timeHit) {
                TimeAlarm alarm = result.getAlarm();
                if (result.isAlarmHit() && alarm.getScalingPolicy() != null) {
                    Cluster cluster = result.getCluster();
                    LOGGER.info(cluster.getId(), "Time ({}) related alarm triggers a scaling event", alarm.getName());
                    handleNotifications(cluster, alarm);
                    handleScaling(cluster, alarm);
                }
            }
        } else {
            MetricHandler metricHandler = metricHandlers.get(eventType);
            List<MetricResult> alarmHit = metricHandler.isAlarmHit((ApplicationEvent) typedEvent);
            for (MetricResult result : alarmHit) {
                MetricAlarm alarm = result.getAlarm();
                if (result.isAlarmHit() && alarm.getScalingPolicy() != null) {
                    Cluster cluster = result.getCluster();
                    LOGGER.info(cluster.getId(), "Metric ({}) related alarm triggers a scaling event", alarm.getName());
                    handleNotifications(cluster, alarm);
                    handleScaling(cluster, alarm);
                }
            }
        }

    }

    private void handleScaling(Cluster cluster, BaseAlarm alarm) {
        ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
        if (scalingPolicy != null) {
            scalingService.scale(cluster, scalingPolicy);
        }
    }

    private void handleNotifications(Cluster cluster, BaseAlarm alarm) {
        if (!alarm.isNotificationSent()) {
            for (Notification notification : alarm.getNotifications()) {
                notificationService.sendNotification(cluster, alarm, notification);
            }
            alarm.setNotificationSent(true);
        }
    }

}