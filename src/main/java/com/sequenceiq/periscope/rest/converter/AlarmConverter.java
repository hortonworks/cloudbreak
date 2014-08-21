package com.sequenceiq.periscope.rest.converter;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Alarm;
import com.sequenceiq.periscope.domain.Notification;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.rest.json.AlarmJson;
import com.sequenceiq.periscope.rest.json.NotificationJson;

@Component
public class AlarmConverter extends AbstractConverter<AlarmJson, Alarm> {

    @Autowired
    private NotificationConverter notificationConverter;

    @Override
    public Alarm convert(AlarmJson source) {
        Alarm alarm = new Alarm();
        alarm.setName(source.getAlarmName());
        alarm.setComparisonOperator(source.getComparisonOperator());
        alarm.setDescription(source.getDescription());
        alarm.setMetric(source.getMetric());
        alarm.setPeriod(source.getPeriod());
        alarm.setThreshold(source.getThreshold());
        List<NotificationJson> notifications = source.getNotifications();
        if (notifications != null && !notifications.isEmpty()) {
            alarm.setNotifications(notificationConverter.convertAllFromJson(notifications));
        }
        return alarm;
    }

    @Override
    public AlarmJson convert(Alarm source) {
        AlarmJson json = new AlarmJson();
        json.setId(source.getId());
        ScalingPolicy scalingPolicy = source.getScalingPolicy();
        json.setScalingPolicyId(scalingPolicy == null ? null : scalingPolicy.getId());
        json.setAlarmName(source.getName());
        json.setComparisonOperator(source.getComparisonOperator());
        json.setDescription(source.getDescription());
        json.setMetric(source.getMetric());
        json.setPeriod(source.getPeriod());
        json.setThreshold(source.getThreshold());
        List<Notification> notifications = source.getNotifications();
        notifications = notifications == null ? Collections.<Notification>emptyList() : notifications;
        json.setNotifications(notificationConverter.convertAllToJson(notifications));
        return json;
    }

}
