package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.PrometheusAlertJson;
import com.sequenceiq.periscope.domain.PrometheusAlert;

@Component
public class PrometheusAlertConverter extends AbstractConverter<PrometheusAlertJson, PrometheusAlert> {

    @Override
    public PrometheusAlert convert(PrometheusAlertJson source) {
        PrometheusAlert alert = new PrometheusAlert();
        alert.setName(source.getAlertName());
        alert.setDescription(source.getDescription());
        alert.setAlertRule(source.getAlertRule());
        alert.setPeriod(source.getPeriod());
        alert.setAlertState(source.getAlertState());
        return alert;
    }

    @Override
    public PrometheusAlertJson convert(PrometheusAlert source) {
        PrometheusAlertJson json = new PrometheusAlertJson();
        json.setId(source.getId());
        json.setScalingPolicyId(source.getScalingPolicyId());
        json.setAlertName(source.getName());
        json.setDescription(source.getDescription());
        json.setPeriod(source.getPeriod());
        json.setAlertRule(source.getAlertRule());
        json.setAlertState(source.getAlertState());
        return json;
    }
}
