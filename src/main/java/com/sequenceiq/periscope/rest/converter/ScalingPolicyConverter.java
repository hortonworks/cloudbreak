package com.sequenceiq.periscope.rest.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.Alarm;
import com.sequenceiq.periscope.model.ScalingPolicy;
import com.sequenceiq.periscope.repository.AlarmRepository;
import com.sequenceiq.periscope.rest.json.ScalingPolicyJson;

@Component
public class ScalingPolicyConverter extends AbstractConverter<ScalingPolicyJson, ScalingPolicy> {

    @Autowired
    private AlarmRepository alarmRepository;

    @Override
    public ScalingPolicy convert(ScalingPolicyJson source) {
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(source.getAdjustmentType());
        Long alarmId = source.getAlarmId();
        if (alarmId != null) {
            Alarm alarm = alarmRepository.findOne(alarmId);
            policy.setAlarm(alarm);
            alarm.setScalingPolicy(policy);
        }
        policy.setName(source.getName());
        policy.setScalingAdjustment(source.getScalingAdjustment());
        return policy;
    }

    @Override
    public ScalingPolicyJson convert(ScalingPolicy source) {
        ScalingPolicyJson json = new ScalingPolicyJson();
        json.setId(source.getId());
        json.setAdjustmentType(source.getAdjustmentType());
        Alarm alarm = source.getAlarm();
        json.setAlarmId(alarm == null ? null : alarm.getId());
        json.setName(source.getName());
        json.setScalingAdjustment(source.getScalingAdjustment());
        return json;
    }
}
