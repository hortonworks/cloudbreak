package com.sequenceiq.periscope.rest.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.MetricAlertJson;
import com.sequenceiq.periscope.domain.MetricAlert;

@Component
public class MetricAlertConverter extends AbstractConverter<MetricAlertJson, MetricAlert> {

    @Inject
    private ScalingPolicyConverter scalingPolicyConverter;

    @Override
    public MetricAlert convert(MetricAlertJson source) {
        MetricAlert alert = new MetricAlert();
        alert.setName(source.getAlertName());
        alert.setDescription(source.getDescription());
        alert.setDefinitionName(source.getAlertDefinition());
        alert.setPeriod(source.getPeriod());
        alert.setAlertState(source.getAlertState());
        if (source.getScalingPolicy() != null) {
            alert.setScalingPolicy(scalingPolicyConverter.convert(source.getScalingPolicy()));
        }
        return alert;
    }

    @Override
    public MetricAlertJson convert(MetricAlert source) {
        MetricAlertJson json = new MetricAlertJson();
        json.setId(source.getId());
        json.setScalingPolicyId(source.getScalingPolicyId());
        json.setAlertName(source.getName());
        json.setDescription(source.getDescription());
        json.setPeriod(source.getPeriod());
        json.setAlertDefinition(source.getDefinitionName());
        json.setAlertState(source.getAlertState());
        if (source.getScalingPolicy() != null) {
            json.setScalingPolicy(scalingPolicyConverter.convert(source.getScalingPolicy()));
        }
        return json;
    }

}
