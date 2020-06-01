package com.sequenceiq.periscope.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.LoadAlertConfigurationResponse;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;

@Component
public class LoadAlertConfigurationResponseConverter extends AbstractConverter<LoadAlertConfigurationResponse, LoadAlertConfiguration> {

    @Override
    public LoadAlertConfigurationResponse convert(LoadAlertConfiguration source) {
        LoadAlertConfigurationResponse json = new LoadAlertConfigurationResponse();
        json.setMaxResourceValue(source.getMaxResourceValue());
        json.setMinResourceValue(source.getMinResourceValue());
        json.setCoolDownMinutes(source.getCoolDownMinutes());
        return json;
    }
}
