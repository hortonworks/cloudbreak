package com.sequenceiq.periscope.rest.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.TimeAlertJson;
import com.sequenceiq.periscope.domain.TimeAlert;

@Component
public class TimeAlertConverter extends AbstractConverter<TimeAlertJson, TimeAlert> {

    @Inject
    private ScalingPolicyConverter scalingPolicyConverter;

    @Override
    public TimeAlert convert(TimeAlertJson source) {
        TimeAlert alarm = new TimeAlert();
        alarm.setName(source.getAlertName());
        alarm.setDescription(source.getDescription());
        alarm.setCron(source.getCron());
        alarm.setTimeZone(source.getTimeZone());
        if (source.getScalingPolicy() != null) {
            alarm.setScalingPolicy(scalingPolicyConverter.convert(source.getScalingPolicy()));
        }
        return alarm;
    }

    @Override
    public TimeAlertJson convert(TimeAlert source) {
        TimeAlertJson json = new TimeAlertJson();
        json.setId(source.getId());
        json.setAlertName(source.getName());
        json.setCron(source.getCron());
        json.setTimeZone(source.getTimeZone());
        json.setDescription(source.getDescription());
        json.setScalingPolicyId(source.getScalingPolicyId());
        if (source.getScalingPolicy() != null) {
            json.setScalingPolicy(scalingPolicyConverter.convert(source.getScalingPolicy()));
        }
        return json;
    }

}
