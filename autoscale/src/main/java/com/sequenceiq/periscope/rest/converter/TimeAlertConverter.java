package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.TimeAlertJson;
import com.sequenceiq.periscope.domain.TimeAlert;

@Component
public class TimeAlertConverter extends AbstractConverter<TimeAlertJson, TimeAlert> {

    @Override
    public TimeAlert convert(TimeAlertJson source) {
        TimeAlert alarm = new TimeAlert();
        alarm.setName(source.getAlertName());
        alarm.setDescription(source.getDescription());
        alarm.setCron(source.getCron());
        alarm.setTimeZone(source.getTimeZone());
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
        return json;
    }

}
