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
import com.sequenceiq.cloudbreak.auth.crn.Crn;
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
        String accountId = Crn.safeFromString(cluster.getStackCrn()).getAccountId();
        if (!distroXAutoscaleClusterRequest.getTimeAlertRequests().isEmpty()) {
            Set<String> timeAlertHostGroups = distroXAutoscaleClusterRequest.getTimeAlertRequests().stream()
                    .map(timeAlertRequest -> {
                        validateSchedule(timeAlertRequest);
                        validateClusterLimit(timeAlertRequest.getScalingPolicy().getAdjustmentType(),
                                timeAlertRequest.getScalingPolicy().getScalingAdjustment(), accountId);
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
                                loadAlertRequest.getLoadAlertConfiguration().getMaxResourceValue(), accountId);
                        return loadAlertRequest;
                    })
                    .map(LoadAlertRequest::getScalingPolicy)
                    .map(ScalingPolicyBase::getHostGroup)
                    .collect(Collectors.toSet());
            validateSupportedHostGroup(cluster, loadAlertHostGroups, AlertType.LOAD);
        }
    }

    public void validateClusterLimit(AdjustmentType adjustmentType, Integer configuredMax, String accountId) {
        if ((adjustmentType == AdjustmentType.LOAD_BASED || adjustmentType == AdjustmentType.EXACT)
                && configuredMax > limitsConfigurationService.getMaxNodeCountLimit(accountId)) {
            throw new BadRequestException(messagesService.getMessage(MessageCode.AUTOSCALING_CLUSTER_LIMIT_EXCEEDED,
                    List.of(limitsConfigurationService.getMaxNodeCountLimit(accountId))));
        }
    }

    public void validateScheduleWithStopStart(DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        if (Boolean.TRUE.equals(autoscaleClusterRequest.getUseStopStartMechanism())
                && !autoscaleClusterRequest.getTimeAlertRequests().isEmpty()) {
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
            dateService.getCronExpression(json.getCron(), json.getTimeZone());
        } catch (ParseException parseException) {
            throw new BadRequestException(parseException.getMessage(), parseException);
        }
    }

    @VisibleForTesting
    protected void setDateService(DateService dateService) {
        this.dateService = dateService;
    }
}
