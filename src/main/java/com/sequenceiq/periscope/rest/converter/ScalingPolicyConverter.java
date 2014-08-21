package com.sequenceiq.periscope.rest.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Alarm;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.repository.AlarmRepository;
import com.sequenceiq.periscope.repository.ScalingPolicyRepository;
import com.sequenceiq.periscope.rest.json.ScalingPolicyJson;
import com.sequenceiq.periscope.service.AlarmNotFoundException;

@Component
public class ScalingPolicyConverter extends AbstractConverter<ScalingPolicyJson, ScalingPolicy> {

    @Autowired
    private AlarmRepository alarmRepository;
    @Autowired
    private ScalingPolicyRepository policyRepository;

    @Override
    public ScalingPolicy convert(ScalingPolicyJson source) {
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(source.getAdjustmentType());
        policy.setName(source.getName());
        policy.setScalingAdjustment(source.getScalingAdjustment());
        policy.setHostGroup(source.getHostGroup());
        long alarmId = source.getAlarmId();
        Alarm alarm = alarmRepository.findOne(alarmId);
        if (alarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        policy.setAlarm(alarm);
        alarm.setScalingPolicy(policy);
        policyRepository.save(policy);
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
        json.setHostGroup(source.getHostGroup());
        return json;
    }
}
