package com.sequenceiq.periscope.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.MetricAlertRequest;
import com.sequenceiq.periscope.domain.MetricAlert;

@Component
public class MetricAlertRequestConverter extends AbstractConverter<MetricAlertRequest, MetricAlert> {

    @Inject
    private ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @Override
    public MetricAlert convert(MetricAlertRequest source) {
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
    public MetricAlertRequest convert(MetricAlert source) {
        MetricAlertRequest json = new MetricAlertRequest();
        json.setAlertName(source.getName());
        json.setDescription(source.getDescription());
        json.setPeriod(source.getPeriod());
        json.setAlertDefinition(source.getDefinitionName());
        json.setAlertDefinitionLabel(source.getDefinitionLabel());
        json.setAlertState(source.getAlertState());
        return json;
    }

}
