package com.sequenceiq.periscope.rest.converter;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Notification;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlarm;
import com.sequenceiq.periscope.rest.json.NotificationJson;
import com.sequenceiq.periscope.rest.json.TimeAlarmJson;

@Component
public class TimeAlarmConverter extends AbstractConverter<TimeAlarmJson, TimeAlarm> {

    @Autowired
    private NotificationConverter notificationConverter;

    @Override
    public TimeAlarm convert(TimeAlarmJson source) {
        TimeAlarm alarm = new TimeAlarm();
        alarm.setName(source.getAlarmName());
        alarm.setDescription(source.getDescription());
        alarm.setStartTime(source.getStartTime());
        alarm.setEndTime(source.getEndTime());
        alarm.setTimeZone(source.getTimeZone());
        alarm.setRecurrence(source.getRecurrence());
        List<NotificationJson> notifications = source.getNotifications();
        if (notifications != null && !notifications.isEmpty()) {
            alarm.setNotifications(notificationConverter.convertAllFromJson(notifications));
        }
        return alarm;
    }

    @Override
    public TimeAlarmJson convert(TimeAlarm source) {
        TimeAlarmJson json = new TimeAlarmJson();
        json.setId(source.getId());
        ScalingPolicy scalingPolicy = source.getScalingPolicy();
        json.setScalingPolicyId(scalingPolicy == null ? null : scalingPolicy.getId());
        json.setAlarmName(source.getName());
        json.setStartTime(source.getStartTime());
        json.setEndTime(source.getEndTime());
        json.setTimeZone(source.getTimeZone());
        json.setRecurrence(source.getRecurrence());
        json.setDescription(source.getDescription());
        List<Notification> notifications = source.getNotifications();
        notifications = notifications == null ? Collections.<Notification>emptyList() : notifications;
        json.setNotifications(notificationConverter.convertAllToJson(notifications));
        return json;
    }

}
