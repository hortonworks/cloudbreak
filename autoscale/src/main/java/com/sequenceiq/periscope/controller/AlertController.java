package com.sequenceiq.periscope.controller;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.v1.AlertEndpoint;
import com.sequenceiq.periscope.api.model.AlertRuleDefinitionEntry;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.LoadAlertResponse;
import com.sequenceiq.periscope.api.model.MetricAlertRequest;
import com.sequenceiq.periscope.api.model.MetricAlertResponse;
import com.sequenceiq.periscope.api.model.PrometheusAlertRequest;
import com.sequenceiq.periscope.api.model.PrometheusAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertValidationRequest;
import com.sequenceiq.periscope.converter.LoadAlertRequestConverter;
import com.sequenceiq.periscope.converter.LoadAlertResponseConverter;
import com.sequenceiq.periscope.converter.MetricAlertRequestConverter;
import com.sequenceiq.periscope.converter.MetricAlertResponseConverter;
import com.sequenceiq.periscope.converter.PrometheusAlertRequestConverter;
import com.sequenceiq.periscope.converter.PrometheusAlertResponseConverter;
import com.sequenceiq.periscope.converter.TimeAlertRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertResponseConverter;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.NotFoundException;

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
    private LoadAlertRequestConverter loadAlertRequestConverter;

    @Inject
    private LoadAlertResponseConverter loadAlertResponseConverter;

    @Inject
    private DateService dateService;

    @Inject
    private ClusterService clusterService;

    public MetricAlertResponse createMetricAlerts(Long clusterId, MetricAlertRequest json) {
        MetricAlert metricAlert = metricAlertRequestConverter.convert(json);
        return createMetricAlarmResponse(alertService.createMetricAlert(clusterId, metricAlert));
    }

    public MetricAlertResponse updateMetricAlerts(Long clusterId, Long alertId, MetricAlertRequest json) {
        MetricAlert metricAlert = metricAlertRequestConverter.convert(json);
        return createMetricAlarmResponse(alertService.updateMetricAlert(clusterId, alertId, metricAlert));
    }

    public List<MetricAlertResponse> getMetricAlerts(Long clusterId) {
        return createAlarmsResponse(alertService.getMetricAlerts(clusterId));
    }

    public void deleteMetricAlarm(Long clusterId, Long alertId) {
        alertService.deleteMetricAlert(clusterId, alertId);
    }

    public List<Map<String, Object>> getAlertDefinitions(Long clusterId) {
        return alertService.getAlertDefinitions(clusterId);
    }

    @Override
    public TimeAlertResponse createTimeAlert(Long clusterId, TimeAlertRequest json) {
        validateTimeAlert(clusterId, Optional.empty(), json);
        TimeAlert timeAlert = timeAlertRequestConverter.convert(json);
        return createTimeAlertResponse(alertService.createTimeAlert(clusterId, timeAlert));
    }

    @Override
    public TimeAlertResponse updateTimeAlert(Long clusterId, Long alertId, TimeAlertRequest json) {
        validateTimeAlert(clusterId, Optional.of(alertId), json);
        TimeAlert timeAlert = timeAlertRequestConverter.convert(json);
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
        dateService.getCronExpression(json.getCronExpression());
        return true;
    }

    public PrometheusAlertResponse createPrometheusAlert(Long clusterId, PrometheusAlertRequest json) {
        PrometheusAlert prometheusAlert = prometheusAlertRequestConverter.convert(json);
        return createPrometheusAlertResponse(alertService.createPrometheusAlert(clusterId, prometheusAlert));
    }

    public PrometheusAlertResponse updatePrometheusAlert(Long clusterId, Long alertId, PrometheusAlertRequest json) {
        PrometheusAlert prometheusAlert = prometheusAlertRequestConverter.convert(json);
        return createPrometheusAlertResponse(alertService.updatePrometheusAlert(clusterId, alertId, prometheusAlert));
    }

    public List<PrometheusAlertResponse> getPrometheusAlerts(Long clusterId) {
        return alertService.getPrometheusAlerts(clusterId).stream()
                .map(this::createPrometheusAlertResponse).collect(Collectors.toList());
    }

    public void deletePrometheusAlarm(Long clusterId, Long alertId) {
        alertService.deletePrometheusAlert(clusterId, alertId);
    }

    public List<AlertRuleDefinitionEntry> getPrometheusDefinitions(Long clusterId) {
        return alertService.getPrometheusAlertDefinitions();
    }

    @Override
    public LoadAlertResponse createLoadAlert(Long clusterId, @Valid LoadAlertRequest json) {
        validateLoadAlert(clusterId, Optional.empty(), json);
        LoadAlert loadAlert = loadAlertRequestConverter.convert(json);
        return createLoadAlertResponse(alertService.createLoadAlert(clusterId, loadAlert));
    }

    @Override
    public LoadAlertResponse updateLoadAlert(Long clusterId, Long alertId, @Valid LoadAlertRequest json) {
        validateLoadAlert(clusterId, Optional.of(alertId), json);
        LoadAlert loadAlert = loadAlertRequestConverter.convert(json);
        return createLoadAlertResponse(alertService.updateLoadAlert(clusterId, alertId, loadAlert));
    }

    @Override
    public List<LoadAlertResponse> getLoadAlerts(Long clusterId) {
        return alertService.getLoadAlerts(clusterId).stream()
                .map(this::createLoadAlertResponse).collect(Collectors.toList());
    }

    @Override
    public void deleteLoadAlert(Long clusterId, Long alertId) {
        alertService.deleteLoadAlert(clusterId, alertId);
    }

    public void createLoadAlerts(Long clusterId, List<LoadAlertRequest> loadAlerts) {
        for (LoadAlertRequest loadAlert : loadAlerts) {
            createLoadAlert(clusterId, loadAlert);
        }
    }

    public void createTimeAlerts(Long clusterId, List<TimeAlertRequest> timeAlerts) {
        for (TimeAlertRequest timeAlert : timeAlerts) {
            createTimeAlert(clusterId, timeAlert);
        }
    }

    public void validateLoadAlertRequests(Long clusterId, List<LoadAlertRequest> loadAlertRequests) {
        for (LoadAlertRequest loadAlertRequest : loadAlertRequests) {
            validateLoadAlert(clusterId, Optional.empty(), loadAlertRequest);
        }
    }

    public void validateTimeAlertRequests(Long clusterId, List<TimeAlertRequest> timeAlertRequests) {
        for (TimeAlertRequest timeAlertRequest : timeAlertRequests) {
            validateTimeAlert(clusterId, Optional.empty(), timeAlertRequest);
        }
    }

    protected void setDateService(DateService dateService) {
        this.dateService = dateService;
    }

    private LoadAlertResponse createLoadAlertResponse(LoadAlert loadAlert) {
        return loadAlertResponseConverter.convert(loadAlert);
    }

    private void validateTimeAlert(Long clusterId, Optional<Long> alertId, TimeAlertRequest json) {
        alertId.ifPresent(alert -> validateAlertForUpdate(clusterId, alert, AlertType.TIME));
        try {
            dateService.validateTimeZone(json.getTimeZone());
            dateService.getCronExpression(json.getCron());
        } catch (ParseException parseException) {
            throw new BadRequestException(parseException.getMessage(), parseException);
        }
    }

    private void validateLoadAlert(Long clusterId, Optional<Long> alertId, LoadAlertRequest json) {
        Cluster cluster = clusterService.findById(clusterId);
        alertId.ifPresentOrElse(
                updateAlert -> {
                    validateAlertForUpdate(clusterId, updateAlert, AlertType.LOAD);
                }, () -> {
                    if (!cluster.getTunnel().useClusterProxy()) {
                        throw new BadRequestException(String.format("Cluster '%s' is not configured with Cluster Proxy Tunnel, " +
                                "Cluster Tunnel is %s. Load Based Scaling not supported", cluster.getStackCrn(), cluster.getTunnel()));
                    }

                    String hostGroup = json.getScalingPolicy().getHostGroup();
                    if (!alertService.getLoadAlertsForClusterHostGroup(clusterId, hostGroup).isEmpty()) {
                        String stackCrn = clusterService.findStackCrnById(clusterId);
                        throw new BadRequestException(String.format("LoadAlert is already defined for Cluster %s, HostGroup %s",
                                stackCrn, hostGroup));
                    }
                });
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

    private void validateAlertForUpdate(Long clusterId, Long alertId, AlertType alertType) {
        BaseAlert alert;
        switch (alertType) {
            case LOAD:
                alert = alertService.findLoadAlertByCluster(clusterId, alertId);
                break;
            case TIME:
                alert = alertService.findTimeAlertByCluster(clusterId, alertId);
                break;
            case METRIC:
                alert = alertService.findMetricAlertByCluster(clusterId, alertId);
                break;
            case PROMETHEUS:
                alert = alertService.findPrometheusAlertByCluster(clusterId, alertId);
                break;
            default:
                alert = null;
        }
        if (alert == null) {
            String clusterCrn = clusterService.findStackCrnById(clusterId);
            throw new NotFoundException(String.format("Could not find %s alert with id: '%s', for cluster: '%s'", alertType, alertId, clusterCrn));
        }
    }
}
