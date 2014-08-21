package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Alarm;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.rest.json.AlarmJson;

@Component
public class AlarmConverter extends AbstractConverter<AlarmJson, Alarm> {

    @Override
    public Alarm convert(AlarmJson source) {
        Alarm alarm = new Alarm();
        alarm.setAlarmName(source.getAlarmName());
        alarm.setComparisonOperator(source.getComparisonOperator());
        alarm.setDescription(source.getDescription());
        alarm.setMetric(source.getMetric());
        alarm.setPeriod(source.getPeriod());
        alarm.setThreshold(source.getThreshold());
        return alarm;
    }

    @Override
    public AlarmJson convert(Alarm source) {
        AlarmJson alarmJson = new AlarmJson();
        alarmJson.setId(source.getId());
        ScalingPolicy scalingPolicy = source.getScalingPolicy();
        alarmJson.setScalingPolicyId(scalingPolicy == null ? null : scalingPolicy.getId());
        alarmJson.setAlarmName(source.getAlarmName());
        alarmJson.setComparisonOperator(source.getComparisonOperator());
        alarmJson.setDescription(source.getDescription());
        alarmJson.setMetric(source.getMetric());
        alarmJson.setPeriod(source.getPeriod());
        alarmJson.setThreshold(source.getThreshold());
        return alarmJson;
    }

}
