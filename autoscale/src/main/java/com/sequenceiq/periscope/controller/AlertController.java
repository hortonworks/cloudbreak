package com.sequenceiq.periscope.controller;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.periscope.api.endpoint.v1.AlertEndpoint;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.LoadAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertValidationRequest;
import com.sequenceiq.periscope.converter.LoadAlertRequestConverter;
import com.sequenceiq.periscope.converter.LoadAlertResponseConverter;
import com.sequenceiq.periscope.converter.TimeAlertRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertResponseConverter;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.NotFoundException;

@Controller
@AuthorizationResource
public class AlertController implements AlertEndpoint {

    @Value("${periscope.enabledPlatforms:}")
    private Set<String> supportedCloudPlatforms;

    @Inject
    private AlertService alertService;

    @Inject
    private TimeAlertRequestConverter timeAlertRequestConverter;

    @Inject
    private TimeAlertResponseConverter timeAlertResponseConverter;

    @Inject
    private LoadAlertRequestConverter loadAlertRequestConverter;

    @Inject
    private LoadAlertResponseConverter loadAlertResponseConverter;

    @Inject
    private DateService dateService;

    @Inject
    private ClusterService clusterService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public TimeAlertResponse createTimeAlert(Long clusterId, TimeAlertRequest json) {
        validateTimeAlert(clusterId, Optional.empty(), json);
        TimeAlert timeAlert = timeAlertRequestConverter.convert(json);
        return createTimeAlertResponse(alertService.createTimeAlert(clusterId, timeAlert));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public TimeAlertResponse updateTimeAlert(Long clusterId, Long alertId, TimeAlertRequest json) {
        validateTimeAlert(clusterId, Optional.of(alertId), json);
        TimeAlert timeAlert = timeAlertRequestConverter.convert(json);
        return createTimeAlertResponse(alertService.updateTimeAlert(clusterId, alertId, timeAlert));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_READ)
    public List<TimeAlertResponse> getTimeAlerts(Long clusterId) {
        Set<TimeAlert> timeAlerts = alertService.getTimeAlerts(clusterId);
        return createTimeAlertsResponse(timeAlerts);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public void deleteTimeAlert(Long clusterId, Long alertId) {
        alertService.deleteTimeAlert(clusterId, alertId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_READ)
    public Boolean validateCronExpression(Long clusterId, TimeAlertValidationRequest json) throws ParseException {
        dateService.getCronExpression(json.getCronExpression());
        return true;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public LoadAlertResponse createLoadAlert(Long clusterId, @Valid LoadAlertRequest json) {
        validateLoadAlert(clusterId, Optional.empty(), json);
        LoadAlert loadAlert = loadAlertRequestConverter.convert(json);
        return createLoadAlertResponse(alertService.createLoadAlert(clusterId, loadAlert));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public LoadAlertResponse updateLoadAlert(Long clusterId, Long alertId, @Valid LoadAlertRequest json) {
        validateLoadAlert(clusterId, Optional.of(alertId), json);
        LoadAlert loadAlert = loadAlertRequestConverter.convert(json);
        return createLoadAlertResponse(alertService.updateLoadAlert(clusterId, alertId, loadAlert));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_READ)
    public List<LoadAlertResponse> getLoadAlerts(Long clusterId) {
        return alertService.getLoadAlerts(clusterId).stream()
                .map(this::createLoadAlertResponse).collect(Collectors.toList());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
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

    private void validateCloudPlatform(Cluster cluster) {
        if (!supportedCloudPlatforms.contains(cluster.getCloudPlatform())) {
            throw new BadRequestException(String.format("Autoscaling for CloudPlatform '%s' is not supported, Cluster '%s'",
                    cluster.getCloudPlatform(), cluster.getStackCrn()));
        }
    }

    private void validateTimeAlert(Long clusterId, Optional<Long> alertId, TimeAlertRequest json) {
        Cluster cluster = clusterService.findById(clusterId);
        alertId.ifPresentOrElse(updateAlert -> {
                    validateAlertForUpdate(clusterId, updateAlert, AlertType.TIME);
                }, () -> {
                    validateCloudPlatform(cluster);
                });
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
                    validateCloudPlatform(cluster);

                    String hostGroup = json.getScalingPolicy().getHostGroup();
                    if (!alertService.getLoadAlertsForClusterHostGroup(clusterId, hostGroup).isEmpty()) {
                        String stackCrn = clusterService.findStackCrnById(clusterId);
                        throw new BadRequestException(String.format("LoadAlert is already defined for Cluster %s, HostGroup %s",
                                stackCrn, hostGroup));
                    }
                });
    }

    private List<TimeAlertResponse> createTimeAlertsResponse(Set<TimeAlert> alarms) {
        List<TimeAlert> metricAlarms = new ArrayList<>(alarms);
        return createTimeAlertsResponse(metricAlarms);
    }

    private TimeAlertResponse createTimeAlertResponse(TimeAlert alarm) {
        return timeAlertResponseConverter.convert(alarm);
    }

    private List<TimeAlertResponse> createTimeAlertsResponse(List<TimeAlert> alarms) {
        return timeAlertResponseConverter.convertAllToJson(alarms);
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
