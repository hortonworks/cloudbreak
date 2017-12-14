package com.sequenceiq.periscope.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.domain.TimeAlert;

@Component
public class TimeAlertResponseConverter extends AbstractConverter<TimeAlertResponse, TimeAlert> {

    @Inject
    private ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @Override
    public TimeAlert convert(TimeAlertResponse source) {
        TimeAlert alarm = new TimeAlert();
        alarm.setName(source.getAlertName());
        alarm.setDescription(source.getDescription());
        alarm.setCron(source.getCron());
        alarm.setTimeZone(source.getTimeZone());
        if (source.getScalingPolicy() != null) {
            alarm.setScalingPolicy(scalingPolicyRequestConverter.convert(source.getScalingPolicy()));
        }
        return alarm;
    }

    @Override
    public TimeAlertResponse convert(TimeAlert source) {
        TimeAlertResponse json = new TimeAlertResponse();
        json.setAlertName(source.getName());
        json.setCron(source.getCron());
        json.setTimeZone(source.getTimeZone());
        json.setDescription(source.getDescription());
        json.setScalingPolicyId(source.getScalingPolicyId());
        if (source.getScalingPolicy() != null) {
            json.setScalingPolicy(scalingPolicyRequestConverter.convert(source.getScalingPolicy()));
        }
        return json;
    }

}
