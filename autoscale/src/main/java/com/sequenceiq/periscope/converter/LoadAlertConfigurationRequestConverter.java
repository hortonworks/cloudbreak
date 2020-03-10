package com.sequenceiq.periscope.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.LoadAlertConfigurationRequest;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;

@Component
public class LoadAlertConfigurationRequestConverter extends AbstractConverter<LoadAlertConfigurationRequest, LoadAlertConfiguration> {

    @Override
    public LoadAlertConfiguration convert(LoadAlertConfigurationRequest source) {
        LoadAlertConfiguration json = new LoadAlertConfiguration();
        json.setMaxResourceValue(source.getMaxResourceValue());
        json.setMinResourceValue(source.getMinResourceValue());
        return json;
    }
}
