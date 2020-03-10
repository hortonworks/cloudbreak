package com.sequenceiq.periscope.controller;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.v1.AlertEndpoint;
import com.sequenceiq.periscope.api.model.AlertRuleDefinitionEntry;
import com.sequenceiq.periscope.api.model.MetricAlertRequest;
import com.sequenceiq.periscope.api.model.MetricAlertResponse;
import com.sequenceiq.periscope.api.model.PrometheusAlertRequest;
import com.sequenceiq.periscope.api.model.PrometheusAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertValidationRequest;
import com.sequenceiq.periscope.converter.MetricAlertRequestConverter;
import com.sequenceiq.periscope.converter.MetricAlertResponseConverter;
import com.sequenceiq.periscope.converter.PrometheusAlertRequestConverter;
import com.sequenceiq.periscope.converter.PrometheusAlertResponseConverter;
import com.sequenceiq.periscope.converter.TimeAlertRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertResponseConverter;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.service.DateService;

@Component
public class AlertController implements AlertEndpoint {

    @Inject
    private AlertService alertService;

    @Inject
    private MetricAlertRequestConverter metricAlertRequestConverter;

    @Inject
    private MetricAlertResponseConverter metricAlertResponseConverter;

    @Inject
    private TimeAlertRequestConverter timeAlertRequestConverter;

    @Inject
    private TimeAlertResponseConverter timeAlertResponseConverter;

    @Inject
    private PrometheusAlertRequestConverter prometheusAlertRequestConverter;

    @Inject
    private PrometheusAlertResponseConverter prometheusAlertResponseConverter;

    @Inject
    private DateService dateService;

    @Override
    public MetricAlertResponse createMetricAlerts(Long clusterId, MetricAlertRequest json) {
        MetricAlert metricAlert = metricAlertRequestConverter.convert(json);
        return createMetricAlarmResponse(alertService.createMetricAlert(clusterId, metricAlert));
    }

    @Override
    public MetricAlertResponse updateMetricAlerts(Long clusterId, Long alertId, MetricAlertRequest json) {
        MetricAlert metricAlert = metricAlertRequestConverter.convert(json);
        return createMetricAlarmResponse(alertService.updateMetricAlert(clusterId, alertId, metricAlert));
    }

    @Override
    public List<MetricAlertResponse> getMetricAlerts(Long clusterId) {
        return createAlarmsResponse(alertService.getMetricAlerts(clusterId));
    }

    @Override
    public void deleteMetricAlarm(Long clusterId, Long alertId) {
        alertService.deleteMetricAlert(clusterId, alertId);
    }

    @Override
    public List<Map<String, Object>> getAlertDefinitions(Long clusterId) {
        return alertService.getAlertDefinitions(clusterId);
    }

    @Override
    public TimeAlertResponse createTimeAlert(Long clusterId, TimeAlertRequest json) throws ParseException {
        TimeAlert timeAlert = validateTimeAlert(json);
        return createTimeAlertResponse(alertService.createTimeAlert(clusterId, timeAlert));
    }

    @Override
    public TimeAlertResponse updateTimeAlert(Long clusterId, Long alertId, TimeAlertRequest json)
            throws ParseException {
        TimeAlert timeAlert = validateTimeAlert(json);
        return createTimeAlertResponse(alertService.updateTimeAlert(clusterId, alertId, timeAlert));
    }

    @Override
    public List<TimeAlertResponse> getTimeAlerts(Long clusterId) {
        Set<TimeAlert> timeAlerts = alertService.getTimeAlerts(clusterId);
        return createTimeAlertsResponse(timeAlerts);
    }

    @Override
    public void deleteTimeAlert(Long clusterId, Long alertId) {
        alertService.deleteTimeAlert(clusterId, alertId);
    }

    @Override
    public Boolean validateCronExpression(Long clusterId, TimeAlertValidationRequest json) throws ParseException {
        dateService.validateCronExpression(json.getCronExpression());
        return true;
    }

    @Override
    public PrometheusAlertResponse createPrometheusAlert(Long clusterId, PrometheusAlertRequest json) {
        PrometheusAlert prometheusAlert = prometheusAlertRequestConverter.convert(json);
        return createPrometheusAlertResponse(alertService.createPrometheusAlert(clusterId, prometheusAlert));
    }

    @Override
    public PrometheusAlertResponse updatePrometheusAlert(Long clusterId, Long alertId, PrometheusAlertRequest json) {
        PrometheusAlert prometheusAlert = prometheusAlertRequestConverter.convert(json);
        return createPrometheusAlertResponse(alertService.updatePrometheusAlert(clusterId, alertId, prometheusAlert));
    }

    @Override
    public List<PrometheusAlertResponse> getPrometheusAlerts(Long clusterId) {
        return alertService.getPrometheusAlerts(clusterId).stream()
                .map(this::createPrometheusAlertResponse).collect(Collectors.toList());
    }

    @Override
    public void deletePrometheusAlarm(Long clusterId, Long alertId) {
        alertService.deletePrometheusAlert(clusterId, alertId);
    }

    @Override
    public List<AlertRuleDefinitionEntry> getPrometheusDefinitions(Long clusterId) {
        return alertService.getPrometheusAlertDefinitions();
    }

    private TimeAlert validateTimeAlert(TimeAlertRequest json) throws ParseException {
        TimeAlert alert = timeAlertRequestConverter.convert(json);
        dateService.validateCronExpression(alert.getCron());
        return alert;
    }

    private List<MetricAlertResponse> createAlarmsResponse(Set<MetricAlert> alerts) {
        List<MetricAlert> metricAlerts = new ArrayList<>(alerts);
        return metricAlertResponseConverter.convertAllToJson(metricAlerts);
    }

    private List<TimeAlertResponse> createTimeAlertsResponse(Set<TimeAlert> alarms) {
        List<TimeAlert> metricAlarms = new ArrayList<>(alarms);
        return createTimeAlertsResponse(metricAlarms);
    }

    private MetricAlertResponse createMetricAlarmResponse(MetricAlert alert) {
        return metricAlertResponseConverter.convert(alert);
    }

    private TimeAlertResponse createTimeAlertResponse(TimeAlert alarm) {
        return timeAlertResponseConverter.convert(alarm);
    }

    private List<TimeAlertResponse> createTimeAlertsResponse(List<TimeAlert> alarms) {
        return timeAlertResponseConverter.convertAllToJson(alarms);
    }

    private PrometheusAlertResponse createPrometheusAlertResponse(PrometheusAlert alarm) {
        return prometheusAlertResponseConverter.convert(alarm);
    }
}
