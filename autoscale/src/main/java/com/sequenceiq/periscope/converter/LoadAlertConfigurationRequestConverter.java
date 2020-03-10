package com.sequenceiq.periscope.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.LoadAlertConfigurationRequest;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;

@Component
public class LoadAlertConfigurationRequestConverter extends AbstractConverter<LoadAlertConfigurationRequest, LoadAlertConfiguration> {

    @Override
    public LoadAlertConfiguration convert(LoadAlertConfigurationRequest source) {
        LoadAlertConfiguration loadAlertConfiguration = new LoadAlertConfiguration();
        loadAlertConfiguration.setMaxResourceValue(source.getMaxResourceValue());
        loadAlertConfiguration.setMinResourceValue(source.getMinResourceValue());
        loadAlertConfiguration.setCoolDownMinutes(source.getCoolDownMinutes());
        return loadAlertConfiguration;
    }
}
