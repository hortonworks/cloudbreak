package com.sequenceiq.periscope.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.MetricAlertResponse;
import com.sequenceiq.periscope.domain.MetricAlert;

@Component
public class MetricAlertResponseConverter extends AbstractConverter<MetricAlertResponse, MetricAlert> {

    @Inject
    private ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @Override
    public MetricAlert convert(MetricAlertResponse source) {
        MetricAlert alert = new MetricAlert();
        alert.setName(source.getAlertName());
        alert.setDescription(source.getDescription());
        alert.setDefinitionName(source.getAlertDefinition());
        alert.setDefinitionLabel(source.getAlertDefinitionLabel());
        alert.setPeriod(source.getPeriod());
        alert.setAlertState(source.getAlertState());
        if (source.getScalingPolicy() != null) {
            alert.setScalingPolicy(scalingPolicyRequestConverter.convert(source.getScalingPolicy()));
        }
        return alert;
    }

    @Override
    public MetricAlertResponse convert(MetricAlert source) {
        MetricAlertResponse json = new MetricAlertResponse();
        json.setId(source.getId());
        json.setScalingPolicyId(source.getScalingPolicyId());
        json.setAlertName(source.getName());
        json.setDescription(source.getDescription());
        json.setPeriod(source.getPeriod());
        json.setAlertDefinition(source.getDefinitionName());
        json.setAlertDefinitionLabel(source.getDefinitionLabel());
        json.setAlertState(source.getAlertState());
        if (source.getScalingPolicy() != null) {
            json.setScalingPolicy(scalingPolicyRequestConverter.convert(source.getScalingPolicy()));
        }
        return json;
    }

}
