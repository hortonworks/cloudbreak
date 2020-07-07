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

import org.springframework.stereotype.Controller;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.endpoint.v1.AlertEndpoint;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.LoadAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertValidationRequest;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.converter.LoadAlertRequestConverter;
import com.sequenceiq.periscope.converter.LoadAlertResponseConverter;
import com.sequenceiq.periscope.converter.TimeAlertRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertResponseConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.service.AutoscaleRecommendationService;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.EntitlementValidationService;
import com.sequenceiq.periscope.service.NotFoundException;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;

@Controller
@AuthorizationResource
public class AlertController implements AlertEndpoint {

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

    @Inject
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    @Inject
    private EntitlementValidationService entitlementValidationService;

    @Inject
    private AutoscaleRecommendationService recommendationService;

    @Inject
    private CloudbreakMessagesService messagesService;

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
        Set<TimeAlert> timeAlerts = getClusterForWorkspace(clusterId).getTimeAlerts();
        return createTimeAlertsResponse(timeAlerts);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public void deleteTimeAlert(Long clusterId, Long alertId) {
        validateAlertForUpdate(getClusterForWorkspace(clusterId), alertId, AlertType.TIME);
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
        return getClusterForWorkspace(clusterId).getLoadAlerts().stream()
                .map(this::createLoadAlertResponse).collect(Collectors.toList());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public void deleteLoadAlert(Long clusterId, Long alertId) {
        validateAlertForUpdate(getClusterForWorkspace(clusterId), alertId, AlertType.LOAD);
        alertService.deleteLoadAlert(clusterId, alertId);
    }

    protected void createLoadAlerts(Long clusterId, List<LoadAlertRequest> loadAlerts) {
        for (LoadAlertRequest loadAlert : loadAlerts) {
            createLoadAlert(clusterId, loadAlert);
        }
    }

    protected void createTimeAlerts(Long clusterId, List<TimeAlertRequest> timeAlerts) {
        for (TimeAlertRequest timeAlert : timeAlerts) {
            createTimeAlert(clusterId, timeAlert);
        }
    }

    protected void validateLoadAlertRequests(Long clusterId, List<LoadAlertRequest> loadAlertRequests) {
        for (LoadAlertRequest loadAlertRequest : loadAlertRequests) {
            validateLoadAlert(clusterId, Optional.empty(), loadAlertRequest);
        }
    }

    protected void validateTimeAlertRequests(Long clusterId, List<TimeAlertRequest> timeAlertRequests) {
        for (TimeAlertRequest timeAlertRequest : timeAlertRequests) {
            validateTimeAlert(clusterId, Optional.empty(), timeAlertRequest);
        }
    }

    private Cluster getClusterForWorkspace(Long clusterId) {
        return clusterService.findOneByClusterIdAndTenant(clusterId, restRequestThreadLocalService.getCloudbreakTenant())
                .orElseThrow(NotFoundException.notFound("cluster", clusterId));
    }

    private void validateAccountEntitlement(Cluster cluster) {
        if (!entitlementValidationService.autoscalingEntitlementEnabled(
                ThreadBasedUserCrnProvider.getUserCrn(),
                ThreadBasedUserCrnProvider.getAccountId(),
                cluster.getCloudPlatform())) {
            throw new BadRequestException(messagesService.getMessage(MessageCode.AUTOSCALING_ENTITLEMENT_NOT_ENABLED,
                    List.of(cluster.getCloudPlatform(),  cluster.getStackName())));
        }
    }

    private void validateSupportedHostGroup(Cluster cluster, String requestHostGroup, AlertType alertType) {
        Set<String> supportedHostGroups = Optional.ofNullable(
                recommendationService.getAutoscaleRecommendations(cluster.getStackCrn())).map(
                recommendation -> {
                    if (AlertType.LOAD.equals(alertType)) {
                        return Optional.ofNullable(recommendation.getLoadBasedHostGroups()).orElse(Set.of());
                    }
                    if (AlertType.TIME.equals(alertType)) {
                        return Optional.ofNullable(recommendation.getTimeBasedHostGroups()).orElse(Set.of());
                    }
                    return Set.of("");
                }).orElse(Set.of(""));
        if (!supportedHostGroups.contains(requestHostGroup)) {
            throw new BadRequestException(messagesService.getMessage(MessageCode.UNSUPPORTED_AUTOSCALING_HOSTGROUP,
                    List.of(requestHostGroup, alertType, cluster.getStackName(), supportedHostGroups)));
        }
    }

    private void validateTimeAlert(Long clusterId, Optional<Long> alertId, TimeAlertRequest json) {
        Cluster cluster = getClusterForWorkspace(clusterId);
        alertId.ifPresentOrElse(updateAlert -> validateAlertForUpdate(cluster, updateAlert, AlertType.TIME),
                () -> {
                    validateAccountEntitlement(cluster);
                    validateSupportedHostGroup(cluster, json.getScalingPolicy().getHostGroup(), AlertType.TIME);
                });
        try {
            dateService.validateTimeZone(json.getTimeZone());
            dateService.getCronExpression(json.getCron());
        } catch (ParseException parseException) {
            throw new BadRequestException(parseException.getMessage(), parseException);
        }
    }

    private void validateLoadAlert(Long clusterId, Optional<Long> alertId, LoadAlertRequest json) {
        Cluster cluster = getClusterForWorkspace(clusterId);
        alertId.ifPresentOrElse(updateAlert -> validateAlertForUpdate(cluster, updateAlert, AlertType.LOAD),
                () -> {
                    validateAccountEntitlement(cluster);
                    validateSupportedHostGroup(cluster, json.getScalingPolicy().getHostGroup(), AlertType.LOAD);
                    String requestHostGroup = json.getScalingPolicy().getHostGroup();
                    cluster.getLoadAlerts().stream().map(LoadAlert::getScalingPolicy).map(ScalingPolicy::getHostGroup)
                            .filter(hostGroup -> hostGroup.equalsIgnoreCase(requestHostGroup)).findAny()
                            .ifPresent(hostGroup -> {
                                throw new BadRequestException(messagesService
                                        .getMessage(MessageCode.LOAD_CONFIG_ALREADY_DEFINED, List.of(cluster.getStackName(), requestHostGroup)));
                            });
                    clusterProxyConfigurationService.getClusterProxyUrl()
                            .orElseThrow(() ->  new BadRequestException(
                                    messagesService.getMessage(MessageCode.CLUSTER_PROXY_NOT_CONFIGURED, List.of(cluster.getStackName()))));
                });
    }

    private void validateAlertForUpdate(Cluster cluster, Long alertId, AlertType alertType) {
        Optional alert;
        switch (alertType) {
            case LOAD:
                alert = cluster.getLoadAlerts().stream().map(LoadAlert::getId)
                        .filter(loadAlertId -> loadAlertId.equals(alertId))
                        .findAny();
                break;
            case TIME:
                alert = cluster.getTimeAlerts().stream().map(TimeAlert::getId)
                        .filter(timeAlertId -> timeAlertId.equals(alertId))
                        .findAny();
                break;

            default:
                //Prometheus and Metrics Alerts not supported.
                throw new BadRequestException(messagesService
                        .getMessage(MessageCode.UNSUPPORTED_AUTOSCALING_TYPE, List.of(alertType, cluster.getStackName())));
        }

        if (alert.isEmpty()) {
            throw new NotFoundException(
                    messagesService.getMessage(MessageCode.AUTOSCALING_CONFIG_NOT_FOUND, List.of(alertType, alertId, cluster.getStackName())));
        }
    }

    private List<TimeAlertResponse> createTimeAlertsResponse(Set<TimeAlert> alarms) {
        List<TimeAlert> timeAlerts = new ArrayList<>(alarms);
        return createTimeAlertsResponse(timeAlerts);
    }

    private TimeAlertResponse createTimeAlertResponse(TimeAlert alarm) {
        return timeAlertResponseConverter.convert(alarm);
    }

    private List<TimeAlertResponse> createTimeAlertsResponse(List<TimeAlert> alarms) {
        return timeAlertResponseConverter.convertAllToJson(alarms);
    }

    private LoadAlertResponse createLoadAlertResponse(LoadAlert loadAlert) {
        return loadAlertResponseConverter.convert(loadAlert);
    }

    @VisibleForTesting
    protected void setDateService(DateService dateService) {
        this.dateService = dateService;
    }
}
