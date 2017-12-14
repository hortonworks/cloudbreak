package com.sequenceiq.periscope.converter;

import static com.sequenceiq.periscope.api.model.AlertState.CRITICAL;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.periscope.api.model.AlertOperator;
import com.sequenceiq.periscope.api.model.PrometheusAlertRequest;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.model.json.Json;
import com.sequenceiq.periscope.service.PrometheusAlertTemplateService;

@Component
public class PrometheusAlertRequestConverter extends AbstractConverter<PrometheusAlertRequest, PrometheusAlert> {

    private static final String THRESHOLD_PARAM_KEY = "threshold";

    private static final String OPERATOR_PARAM_KEY = "operator";

    @Inject
    private PrometheusAlertTemplateService templateService;

    @Inject
    private ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @Override
    public PrometheusAlert convert(PrometheusAlertRequest source) {
        PrometheusAlert alert = new PrometheusAlert();
        alert.setName(source.getAlertName());
        alert.setDescription(source.getDescription());
        alert.setPeriod(source.getPeriod());
        alert.setAlertState(source.getAlertState() != null ? source.getAlertState() : CRITICAL);
        double threshold = source.getThreshold();
        String alertRuleName = source.getAlertRuleName();
        try {
            AlertOperator alertOperator = source.getAlertOperator() != null ? source.getAlertOperator() : AlertOperator.MORE_THAN;
            String operator = alertOperator.getOperator();
            String alertRule = templateService.createAlert(alertRuleName, alert.getName(), String.valueOf(threshold), alert.getPeriod(), operator);
            alert.setAlertRule(alertRule);
            alert.setParameters(createParametersFrom(threshold, alertOperator));
        } catch (Exception e) {
            throw new ConversionFailedException(
                    TypeDescriptor.valueOf(PrometheusAlertRequest.class),
                    TypeDescriptor.valueOf(PrometheusAlert.class),
                    source.toString(),
                    e);
        }
        return alert;
    }

    @Override
    public PrometheusAlertRequest convert(PrometheusAlert source) {
        PrometheusAlertRequest json = new PrometheusAlertRequest();
        json.setAlertName(source.getName());
        json.setDescription(source.getDescription());
        json.setPeriod(source.getPeriod());
        json.setAlertRuleName(source.getAlertRule());
        json.setAlertState(source.getAlertState());

        Map<String, Object> parameters = source.getParameters().getMap();
        json.setAlertOperator(AlertOperator.valueOf(String.valueOf(parameters.get(OPERATOR_PARAM_KEY))));
        json.setThreshold(Double.valueOf(String.valueOf(parameters.get(THRESHOLD_PARAM_KEY))));
        return json;
    }

    private Json createParametersFrom(double threshold, AlertOperator operator) throws JsonProcessingException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(THRESHOLD_PARAM_KEY, String.valueOf(threshold));
        parameters.put(OPERATOR_PARAM_KEY, operator.name());
        return new Json(parameters);
    }
}
