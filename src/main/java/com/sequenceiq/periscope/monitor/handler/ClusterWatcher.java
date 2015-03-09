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
import com.sequenceiq.periscope.domain.Notification;
import com.sequenceiq.periscope.domain.ScalingPolicy;
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
        handleEvent(eventType, (ApplicationEvent) typedEvent);
    }

    private void handleEvent(EventType eventType, ApplicationEvent applicationEvent) {
        if (EventType.TIME.equals(eventType)) {
            handleTimeEvent(applicationEvent);
        } else {
            handleMetricEvent(eventType, applicationEvent);
        }
    }

    private void handleTimeEvent(ApplicationEvent applicationEvent) {
        List<TimeResult> timeHit = timeHandler.isTrigger(applicationEvent);
        for (TimeResult result : timeHit) {
            triggerScaling(result, result.getAlarm());
        }
    }

    private void handleMetricEvent(EventType eventType, ApplicationEvent applicationEvent) {
        MetricHandler metricHandler = metricHandlers.get(eventType);
        List<MetricResult> alarmHit = metricHandler.isAlarmHit(applicationEvent);
        for (MetricResult result : alarmHit) {
            triggerScaling(result, result.getAlarm());
        }
    }

    private void triggerScaling(BaseResult result, BaseAlarm alarm) {
        if (result.isAlarmHit() && alarm.getScalingPolicy() != null) {
            Cluster cluster = result.getCluster();
            LOGGER.info(cluster.getId(), "Alarm ({}) triggers a scaling event", alarm.getName());
            scale(cluster, alarm);
        }
    }

    private void scale(Cluster cluster, BaseAlarm alarm) {
        ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
        sendNotifications(cluster, alarm);
        scalingService.scale(cluster, scalingPolicy);
    }

    private void sendNotifications(Cluster cluster, BaseAlarm alarm) {
        if (!alarm.isNotificationSent()) {
            for (Notification notification : alarm.getNotifications()) {
                notificationService.sendNotification(cluster, alarm, notification);
            }
            alarm.setNotificationSent(true);
        }
    }

}