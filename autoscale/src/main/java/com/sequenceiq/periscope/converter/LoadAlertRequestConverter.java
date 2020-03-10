package com.sequenceiq.periscope.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.domain.LoadAlert;

@Component
public class LoadAlertRequestConverter extends AbstractConverter<LoadAlertRequest, LoadAlert> {

    @Inject
    private ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @Inject
    private LoadAlertConfigurationRequestConverter loadAlertConfigurationRequestConverter;

    @Override
    public LoadAlert convert(LoadAlertRequest source) {
        LoadAlert alert = new LoadAlert();
        alert.setName(source.getAlertName());
        alert.setDescription(source.getDescription());

        if (source.getScalingPolicy() != null) {
            alert.setScalingPolicy(scalingPolicyRequestConverter.convert(source.getScalingPolicy()));
        }

        if (source.getLoadAlertConfiguration() != null) {
            alert.setLoadAlertConfiguration(
                    loadAlertConfigurationRequestConverter.convert(source.getLoadAlertConfiguration()));
        }
        return alert;
    }
}
