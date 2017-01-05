package com.sequenceiq.periscope.rest.converter;

import static com.sequenceiq.periscope.api.model.AlertState.CRITICAL;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.PrometheusAlertJson;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.service.PrometheusAlertTemplateService;

@Component
public class PrometheusAlertConverter extends AbstractConverter<PrometheusAlertJson, PrometheusAlert> {

    @Inject
    private PrometheusAlertTemplateService prometheusAlertTemplateService;

    @Override
    public PrometheusAlert convert(PrometheusAlertJson source) {
        PrometheusAlert alert = new PrometheusAlert();
        alert.setName(source.getAlertName());
        alert.setDescription(source.getDescription());
        alert.setPeriod(source.getPeriod());
        alert.setAlertState(source.getAlertState() != null ? source.getAlertState() : CRITICAL);
        double threshold = source.getThreshold();
        String alertRuleName = source.getAlertRuleName();
        try {
            String alertRule = prometheusAlertTemplateService.createAlert(alertRuleName, alert.getName(), String.valueOf(threshold), alert.getPeriod());
            alert.setAlertRule(alertRule);
        } catch (Exception e) {
            throw new ConversionFailedException(
                    TypeDescriptor.valueOf(PrometheusAlertJson.class),
                    TypeDescriptor.valueOf(PrometheusAlert.class),
                    source.toString(),
                    e);
        }
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
        json.setAlertRuleName(source.getAlertRule());
        json.setAlertState(source.getAlertState());
        return json;
    }
}
