package com.sequenceiq.periscope.controller.validation;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyBase;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.controller.AutoScaleClusterCommonService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.AutoscaleRecommendationService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.EntitlementValidationService;
import com.sequenceiq.periscope.service.configuration.LimitsConfigurationService;

@Component
public class AlertValidator {

    @Inject
    private EntitlementValidationService entitlementValidationService;

    @Inject
    private AutoscaleRecommendationService recommendationService;

    @Inject
    private DateService dateService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private AutoScaleClusterCommonService asClusterCommonService;

    @Inject
    private LimitsConfigurationService limitsConfigurationService;

    public void validateEntitlementAndDisableIfNotEntitled(Cluster cluster) {
        if (!entitlementValidationService.autoscalingEntitlementEnabled(ThreadBasedUserCrnProvider.getAccountId(), cluster.getCloudPlatform())) {
            if (cluster.getAutoscalingEnabled()) {
                asClusterCommonService.setAutoscaleState(cluster.getId(), false);
            }
            throw new BadRequestException(messagesService.getMessage(MessageCode.AUTOSCALING_ENTITLEMENT_NOT_ENABLED,
                    List.of(cluster.getCloudPlatform(), cluster.getStackName())));
        }
    }

    public void validateStopStartEntitlementAndDisableIfNotEntitled(Cluster cluster) {
        if (!entitlementValidationService.stopStartAutoscalingEntitlementEnabled(ThreadBasedUserCrnProvider.getAccountId(), cluster.getCloudPlatform())) {
            if (Boolean.TRUE.equals(cluster.isStopStartScalingEnabled())) {
                asClusterCommonService.setStopStartScalingState(cluster.getId(), false);
            }
            throw new BadRequestException(messagesService.getMessage(MessageCode.AUTOSCALING_STOP_START_ENTITLEMENT_NOT_ENABLED,
                    List.of(cluster.getCloudPlatform(), cluster.getStackName())));
        }
    }

    public void validateSupportedHostGroup(Cluster cluster, Set<String> requestHostGroups, AlertType alertType) {
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

        if (!supportedHostGroups.containsAll(requestHostGroups)) {
            throw new BadRequestException(messagesService.getMessage(MessageCode.UNSUPPORTED_AUTOSCALING_HOSTGROUP,
                    List.of(requestHostGroups, alertType, cluster.getStackName(), supportedHostGroups)));
        }
    }

    public void validateDistroXAutoscaleClusterRequest(Cluster cluster,
            DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest) {

        if (!distroXAutoscaleClusterRequest.getTimeAlertRequests().isEmpty()) {
            Set<String> timeAlertHostGroups = distroXAutoscaleClusterRequest.getTimeAlertRequests().stream()
                    .map(timeAlertRequest -> {
                        validateSchedule(timeAlertRequest);
                        validateClusterLimit(timeAlertRequest.getScalingPolicy().getAdjustmentType(),
                                timeAlertRequest.getScalingPolicy().getScalingAdjustment());
                        return timeAlertRequest;
                    })
                    .map(TimeAlertRequest::getScalingPolicy)
                    .map(ScalingPolicyBase::getHostGroup)
                    .collect(Collectors.toSet());
            validateSupportedHostGroup(cluster, timeAlertHostGroups, AlertType.TIME);
        }

        if (!distroXAutoscaleClusterRequest.getLoadAlertRequests().isEmpty()) {
            Set<String> loadAlertHostGroups = distroXAutoscaleClusterRequest.getLoadAlertRequests().stream()
                    .map(loadAlertRequest -> {
                        validateClusterLimit(loadAlertRequest.getScalingPolicy().getAdjustmentType(),
                                loadAlertRequest.getLoadAlertConfiguration().getMaxResourceValue());
                        return loadAlertRequest;
                    })
                    .map(LoadAlertRequest::getScalingPolicy)
                    .map(ScalingPolicyBase::getHostGroup)
                    .collect(Collectors.toSet());
            validateSupportedHostGroup(cluster, loadAlertHostGroups, AlertType.LOAD);
        }
    }

    public void validateClusterLimit(AdjustmentType adjustmentType, Integer configuredMax) {
        if ((adjustmentType == AdjustmentType.LOAD_BASED || adjustmentType == AdjustmentType.EXACT)
                && configuredMax > limitsConfigurationService.getMaxNodeCountLimit()) {
            throw new BadRequestException(messagesService.getMessage(MessageCode.AUTOSCALING_CLUSTER_LIMIT_EXCEEDED,
                    List.of(limitsConfigurationService.getMaxNodeCountLimit())));
        }
    }

    public void validateScheduleWithStopStart(Cluster cluster, DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        if (autoscaleClusterRequest.getUseStopStartMechanism() == null) {
            if (newOrPreExistingTimeAlerts(cluster, autoscaleClusterRequest) && Boolean.TRUE.equals(cluster.isStopStartScalingEnabled())) {
                throw new BadRequestException(messagesService.getMessage(MessageCode.VALIDATION_TIME_STOP_START_UNSUPPORTED));
            }
        } else if (Boolean.TRUE.equals(autoscaleClusterRequest.getUseStopStartMechanism())
                && newOrPreExistingTimeAlerts(cluster, autoscaleClusterRequest)) {
            throw new BadRequestException(messagesService.getMessage(MessageCode.VALIDATION_TIME_STOP_START_UNSUPPORTED));
        }
    }

    public void validateScheduleWithStopStart(Cluster cluster, AutoscaleClusterState autoscaleState) {
        if (Boolean.TRUE.equals(autoscaleState.getUseStopStartMechanism()) && !cluster.getTimeAlerts().isEmpty()) {
            throw new BadRequestException(messagesService.getMessage(MessageCode.VALIDATION_TIME_STOP_START_UNSUPPORTED));
        }
    }

    public void validateSchedule(TimeAlertRequest json) {
        try {
            dateService.validateTimeZone(json.getTimeZone());
            dateService.getCronExpression(json.getCron());
        } catch (ParseException parseException) {
            throw new BadRequestException(parseException.getMessage(), parseException);
        }
    }

    private boolean newOrPreExistingTimeAlerts(Cluster cluster, DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest) {
        return !(cluster.getTimeAlerts().isEmpty() && distroXAutoscaleClusterRequest.getTimeAlertRequests().isEmpty());
    }

    @VisibleForTesting
    protected void setDateService(DateService dateService) {
        this.dateService = dateService;
    }
}
