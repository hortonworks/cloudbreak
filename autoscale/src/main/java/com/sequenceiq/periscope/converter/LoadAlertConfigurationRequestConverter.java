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
        loadAlertConfiguration.setMaxScaleUpStepSize(source.getMaxScaleUpStepSize());
        loadAlertConfiguration.setMaxScaleDownStepSize(source.getMaxScaleDownStepSize());
        loadAlertConfiguration.setCoolDownMinutes(source.getCoolDownMinutes());
        loadAlertConfiguration.setScaleUpCoolDownMinutes(source.getScaleUpCoolDownMinutes());
        loadAlertConfiguration.setScaleDownCoolDownMinutes(source.getScaleDownCoolDownMinutes());
        return loadAlertConfiguration;
    }
}
