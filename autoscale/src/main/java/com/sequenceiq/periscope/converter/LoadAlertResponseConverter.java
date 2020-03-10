package com.sequenceiq.periscope.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.LoadAlertResponse;
import com.sequenceiq.periscope.domain.LoadAlert;

@Component
public class LoadAlertResponseConverter extends AbstractConverter<LoadAlertResponse, LoadAlert> {

    @Inject
    private ScalingPolicyResponseConverter scalingPolicyResponseConverter;

    @Inject
    private LoadAlertConfigurationResponseConverter loadAlertConfigurationResponseConverter;

    @Override
    public LoadAlertResponse convert(LoadAlert source) {
        LoadAlertResponse json = new LoadAlertResponse();
        json.setId(source.getId());
        json.setAlertName(source.getName());
        json.setDescription(source.getDescription());
        if (source.getLoadAlertConfiguration() != null) {
            json.setLoadAlertConfiguration(
                    loadAlertConfigurationResponseConverter.convert(source.getLoadAlertConfiguration()));
        }
        if (source.getScalingPolicy() != null) {
            json.setScalingPolicy(scalingPolicyResponseConverter.convert(source.getScalingPolicy()));
        }
        return json;
    }
}
