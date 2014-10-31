package com.sequenceiq.periscope.rest.converter;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.MetricAlarm;
import com.sequenceiq.periscope.domain.Notification;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.repository.ScalingPolicyRepository;
import com.sequenceiq.periscope.rest.json.MetricAlarmJson;
import com.sequenceiq.periscope.rest.json.NotificationJson;

@Component
public class MetricAlarmConverter extends AbstractConverter<MetricAlarmJson, MetricAlarm> {

    @Autowired
    private NotificationConverter notificationConverter;
    @Autowired
    private ScalingPolicyRepository policyRepository;

    @Override
    public MetricAlarm convert(MetricAlarmJson source) {
        MetricAlarm metricAlarm = new MetricAlarm();
        metricAlarm.setName(source.getAlarmName());
        metricAlarm.setComparisonOperator(source.getComparisonOperator());
        metricAlarm.setDescription(source.getDescription());
        metricAlarm.setMetric(source.getMetric());
        metricAlarm.setPeriod(source.getPeriod());
        metricAlarm.setThreshold(source.getThreshold());
        Long policyId = source.getScalingPolicyId();
        if (policyId != null) {
            ScalingPolicy policy = policyRepository.findOne(policyId);
            if (policy != null) {
                metricAlarm.setScalingPolicy(policy);
                policy.setAlarm(metricAlarm);
            }
        }
        List<NotificationJson> notifications = source.getNotifications();
        if (notifications != null && !notifications.isEmpty()) {
            metricAlarm.setNotifications(notificationConverter.convertAllFromJson(notifications));
        }
        return metricAlarm;
    }

    @Override
    public MetricAlarmJson convert(MetricAlarm source) {
        MetricAlarmJson json = new MetricAlarmJson();
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
