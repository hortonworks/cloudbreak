package com.sequenceiq.periscope.controller;


import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_DISABLED;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_ENABLED;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.AuthorizationEnvironmentCrnProvider;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnProvider;
import com.sequenceiq.cloudbreak.api.model.StatusKind;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.model.NameOrCrn;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.EntitlementValidationService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.NodeDeletionService;
import com.sequenceiq.periscope.service.UsageReportingService;

@Component
public class AutoScaleClusterCommonService implements AuthorizationResourceCrnProvider, AuthorizationEnvironmentCrnProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleClusterCommonService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private HistoryService historyService;

    @Inject
    private UsageReportingService usageReportingService;

    @Inject
    private NodeDeletionService nodeDeletionService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private AlertService alertService;

    @Inject
    private EntitlementValidationService entitlementValidationService;

    public List<Cluster> getDistroXClusters() {
        return clusterService.findDistroXByTenant(restRequestThreadLocalService.getCloudbreakTenant());
    }

    public Cluster getCluster(Long clusterId) {
        return clusterService.findById(clusterId);
    }

    @Retryable(value = NotFoundException.class, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public Cluster getClusterByStackCrn(String stackCrn) {
        return getClusterByCrnOrName(NameOrCrn.ofCrn(stackCrn));
    }

    @Retryable(value = NotFoundException.class, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public Cluster getClusterByStackName(String stackName) {
        return getClusterByCrnOrName(NameOrCrn.ofName(stackName));
    }

    public void deleteCluster(Long clusterId) {
        clusterService.removeById(clusterId);
    }

    public Cluster setState(Long clusterId, StateJson stateJson) {
        Cluster cluster = clusterService.setState(clusterId, stateJson.getState());
        createAutoscalingStateChangedHistoryAndNotify(cluster);
        return cluster;
    }

    public Cluster setAutoscaleState(Long clusterId, AutoscaleClusterState autoscaleState) {
        Cluster cluster = setAutoscaleState(clusterId, autoscaleState.isEnableAutoscaling());
        return setStopStartScalingState(cluster.getId(), autoscaleState.getUseStopStartMechanism(),
                !ObjectUtils.isEmpty(cluster.getTimeAlerts()), !ObjectUtils.isEmpty(cluster.getLoadAlerts()));
    }

    public Cluster setAutoscaleState(Long clusterId, Boolean enableAutoScaling) {
        Cluster cluster = clusterService.findById(clusterId);
        if (enableAutoScaling != null && !Objects.equals(enableAutoScaling, cluster.isAutoscalingEnabled())) {
            cluster = clusterService.setAutoscaleState(clusterId, enableAutoScaling);
        }
        return cluster;
    }

    public Cluster setStopStartScalingState(Long clusterId, Boolean requestedState, boolean hasTimeAlerts, boolean hasLoadAlerts) {
        Cluster cluster = clusterService.findById(clusterId);
        boolean allowedPerEntitlement = canEnableStopStartBasedOnEntitlement(cluster);

        boolean targetState = false;
        if (allowedPerEntitlement && !hasTimeAlerts && hasLoadAlerts) {
            if (requestedState == null || requestedState) {
                targetState = true;
            }
        }
        if (!Boolean.valueOf(targetState).equals(cluster.isStopStartScalingEnabled())) {
            return clusterService.setStopStartScalingState(cluster, targetState);
        }
        return cluster;
    }

    private boolean canEnableStopStartBasedOnEntitlement(Cluster cluster) {
        return entitlementValidationService.stopStartAutoscalingEntitlementEnabled(ThreadBasedUserCrnProvider.getAccountId(), cluster.getCloudPlatform());
    }

    public void deleteAlertsForClusterCrn(String stackCrn) {
        Cluster cluster = getClusterByCrnOrName(NameOrCrn.ofCrn(stackCrn));
        String policyHostGroup = determineLoadBasedPolicyHostGroup(cluster).orElse(null);
        cluster = clusterService.deleteAlertsForCluster(cluster.getId());
        deleteStoppedNodesIfPresent(cluster.getId(), policyHostGroup);
        processAutoscalingAlertsDeleted(cluster);
    }

    public void deleteAlertsForClusterName(String stackName) {
        Cluster cluster = getClusterByCrnOrName(NameOrCrn.ofName(stackName));
        String policyHostGroup = determineLoadBasedPolicyHostGroup(cluster).orElse(null);
        cluster = clusterService.deleteAlertsForCluster(cluster.getId());
        deleteStoppedNodesIfPresent(cluster.getId(), policyHostGroup);
        processAutoscalingAlertsDeleted(cluster);
    }

    public void deleteStoppedNodesIfPresent(Long clusterId, String hostGroup) {
        Cluster cluster = clusterService.findById(clusterId);
        nodeDeletionService.deleteStoppedNodesIfPresent(cluster, hostGroup);
    }

    public void processAutoscalingAlertsDeleted(Cluster cluster) {
        createAutoscalingAlertsDeletedHistoryAndNotify(cluster);
        usageReportingService.reportAutoscalingConfigChanged(restRequestThreadLocalService.getCloudbreakUser().getUserCrn(), cluster);
    }

    public void processAutoscalingConfigChanged(Cluster cluster) {
        createAutoscalingConfigChangedHistoryAndNotify(cluster);
        usageReportingService.reportAutoscalingConfigChanged(restRequestThreadLocalService.getCloudbreakUser().getUserCrn(), cluster);
    }

    public void processAutoscalingStateChanged(Cluster cluster) {
        createAutoscalingStateChangedHistoryAndNotify(cluster);
        usageReportingService.reportAutoscalingConfigChanged(restRequestThreadLocalService.getCloudbreakUser().getUserCrn(), cluster);
    }

    public void createLoadAlerts(Long clusterId, List<LoadAlert> loadAlerts) {
        for (LoadAlert loadAlert : loadAlerts) {
            alertService.createLoadAlert(clusterId, loadAlert);
        }
    }

    public void createTimeAlerts(Long clusterId, List<TimeAlert> timeAlerts) {
        for (TimeAlert timeAlert : timeAlerts) {
            alertService.createTimeAlert(clusterId, timeAlert);
        }
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.DATAHUB;
    }

    @Override
    @Retryable(value = NotFoundException.class, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public String getResourceCrnByResourceName(String clusterName) {
        // All controller methods annotated with @CheckPermissionByResourceName will forcefully sync a cluster to periscope for a successful authz check
        return getClusterByCrnOrName(NameOrCrn.ofName(clusterName)).getStackCrn();
    }

    @Override
    public Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn) {
        return clusterService.findOneByStackCrnAndTenant(resourceCrn, restRequestThreadLocalService.getCloudbreakTenant()).map(Cluster::getEnvironmentCrn);
    }

    public Optional<String> determineLoadBasedPolicyHostGroup(Cluster cluster) {
        Set<LoadAlert> loadAlerts = cluster.getLoadAlerts();
        if (loadAlerts.isEmpty()) {
            LOGGER.info("No loadAlerts found for cluster: {}, policyHostGroup not defined", cluster.getStackCrn());
            return Optional.empty();
        }
        return loadAlerts.stream().map(alert -> alert.getScalingPolicy().getHostGroup()).findAny();
    }

    protected Cluster getClusterByCrnOrName(NameOrCrn nameOrCrn) {
        return nameOrCrn.hasName() ?
                clusterService.findOneByStackNameAndTenant(nameOrCrn.getName(), restRequestThreadLocalService.getCloudbreakTenant())
                        .orElseGet(() -> syncCBClusterByName(nameOrCrn.getName())) :
                clusterService.findOneByStackCrnAndTenant(nameOrCrn.getCrn(), restRequestThreadLocalService.getCloudbreakTenant())
                        .orElseGet(() -> syncCBClusterByCrn(nameOrCrn.getCrn()));
    }

    protected Cluster syncCBClusterByCrn(String stackCrn) {
        return Optional.ofNullable(cloudbreakCommunicator.getAutoscaleClusterByCrn(stackCrn))
                .filter(stack -> WORKLOAD.equals(stack.getStackType()) && stack.getClusterStatus().getStatusKind().equals(StatusKind.FINAL))
                .map(stack -> clusterService.create(stack))
                .orElseThrow(NotFoundException.notFound("cluster", stackCrn));
    }

    protected Cluster syncCBClusterByName(String stackName) {
        String accountId = restRequestThreadLocalService.getCloudbreakTenant();
        return Optional.ofNullable(cloudbreakCommunicator.getAutoscaleClusterByName(stackName, accountId))
                .filter(stack -> WORKLOAD.equals(stack.getStackType()) && stack.getClusterStatus().getStatusKind().equals(StatusKind.FINAL))
                .map(stack -> clusterService.create(stack))
                .orElseThrow(NotFoundException.notFound("cluster", stackName));
    }

    protected void createAutoscalingStateChangedHistoryAndNotify(Cluster cluster) {
        History history;
        history = cluster.isAutoscalingEnabled()
                ? historyService.createEntry(ScalingStatus.ENABLED, messagesService.getMessage(AUTOSCALING_ENABLED), cluster)
                : historyService.createEntry(ScalingStatus.DISABLED, messagesService.getMessage(AUTOSCALING_DISABLED), cluster);
        notificationSender.sendHistoryUpdateNotification(history, cluster);
    }

    protected void createAutoscalingAlertsDeletedHistoryAndNotify(Cluster cluster) {
        ScalingStatus scalingStatus = ScalingStatus.DISABLED;
        String statusMessage = messagesService.getMessage(MessageCode.AUTOSCALING_POLICIES_DELETED);
        notificationSender.sendConfigUpdateNotification(cluster);
        notificationSender.sendHistoryUpdateNotification(historyService.createEntry(scalingStatus, statusMessage, cluster), cluster);
    }

    protected void createAutoscalingConfigChangedHistoryAndNotify(Cluster cluster) {
        ScalingStatus scalingStatus = ScalingStatus.DISABLED;
        String statusMessage = messagesService.getMessage(MessageCode.AUTOSCALING_DISABLED);

        if (!cluster.getLoadAlerts().isEmpty()) {
            String loadBasedHostGroups = cluster.getLoadAlerts().stream()
                    .map(loadAlert -> loadAlert.getScalingPolicy().getHostGroup())
                    .collect(Collectors.joining(","));
            statusMessage = messagesService.getMessage(MessageCode.AUTOSCALING_CONFIG_UPDATED,
                    List.of(AlertType.LOAD, loadBasedHostGroups));
            scalingStatus = ScalingStatus.CONFIG_UPDATED;
        } else if (!cluster.getTimeAlerts().isEmpty()) {
            String timeBasedHostGroups = cluster.getTimeAlerts().stream()
                    .map(timeAlert -> timeAlert.getScalingPolicy().getHostGroup())
                    .distinct()
                    .collect(Collectors.joining(","));
            statusMessage = messagesService.getMessage(MessageCode.AUTOSCALING_CONFIG_UPDATED,
                    List.of(AlertType.TIME, timeBasedHostGroups));
            scalingStatus = ScalingStatus.CONFIG_UPDATED;
        }

        notificationSender.sendConfigUpdateNotification(cluster);
        notificationSender.sendHistoryUpdateNotification(
                historyService.createEntry(scalingStatus, statusMessage, cluster), cluster);
    }
}
