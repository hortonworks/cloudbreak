package com.sequenceiq.periscope.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.domain.TimeAlert;

@Component
public class TimeAlertRequestConverter extends AbstractConverter<TimeAlertRequest, TimeAlert> {

    @Inject
    private ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @Override
    public TimeAlert convert(TimeAlertRequest source) {
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
    public TimeAlertRequest convert(TimeAlert source) {
        TimeAlertRequest json = new TimeAlertRequest();
        json.setAlertName(source.getName());
        json.setCron(source.getCron());
        json.setTimeZone(source.getTimeZone());
        json.setDescription(source.getDescription());
        if (source.getScalingPolicy() != null) {
            json.setScalingPolicy(scalingPolicyRequestConverter.convert(source.getScalingPolicy()));
        }
        return json;
    }
}
