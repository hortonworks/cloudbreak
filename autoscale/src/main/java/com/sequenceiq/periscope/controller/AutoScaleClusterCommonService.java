package com.sequenceiq.periscope.controller;


import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_DISABLED;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_ENABLED;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourceBasedCrnProvider;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.model.NameOrCrn;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.service.AutoscaleRecommendationService;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.EntitlementValidationService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.NotFoundException;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;

@Component
public class AutoScaleClusterCommonService  implements ResourceBasedCrnProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleClusterCommonService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private HistoryService historyService;

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
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    @Inject
    private EntitlementValidationService entitlementValidationService;

    @Inject
    private AutoscaleRecommendationService recommendationService;

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
        createHistoryAndNotification(cluster);
        return cluster;
    }

    public Cluster setAutoscaleState(Long clusterId, AutoscaleClusterState autoscaleState) {
        return setAutoscaleState(clusterId, autoscaleState.isEnableAutoscaling());
    }

    public Cluster setAutoscaleState(Long clusterId, Boolean enableAutoScaling) {
        Cluster cluster = clusterService.findById(clusterId);
        if (!cluster.isAutoscalingEnabled().equals(enableAutoScaling)) {
            cluster = clusterService.setAutoscaleState(clusterId, enableAutoScaling);
            createHistoryAndNotification(cluster);
        }
        return cluster;
    }

    public void deleteAlertsForClusterCrn(String stackCrn) {
        Cluster cluster = getClusterByCrnOrName(NameOrCrn.ofCrn(stackCrn));
        clusterService.deleteAlertsForCluster(cluster.getId());
    }

    public void deleteAlertsForClusterName(String stackName) {
        Cluster cluster = getClusterByCrnOrName(NameOrCrn.ofName(stackName));
        clusterService.deleteAlertsForCluster(cluster.getId());
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
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.DATAHUB;
    }

    @Override
    @Retryable(value = NotFoundException.class, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public String getResourceCrnByResourceName(String clusterName) {
        return getClusterByCrnOrName(NameOrCrn.ofName(clusterName)).getStackCrn();
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
                .filter(stack -> WORKLOAD.equals(stack.getStackType()))
                .map(stack -> clusterService.create(stack))
                .orElseThrow(NotFoundException.notFound("cluster", stackCrn));
    }

    protected Cluster syncCBClusterByName(String stackName) {
        return Optional.ofNullable(cloudbreakCommunicator.getAutoscaleClusterByName(stackName))
                .filter(stack -> WORKLOAD.equals(stack.getStackType()))
                .map(stack -> clusterService.create(stack))
                .orElseThrow(NotFoundException.notFound("cluster", stackName));
    }

    protected void createHistoryAndNotification(Cluster cluster) {
        History history;
        history = cluster.isAutoscalingEnabled()
                ? historyService.createEntry(ScalingStatus.ENABLED, messagesService.getMessage(AUTOSCALING_ENABLED), cluster)
                : historyService.createEntry(ScalingStatus.DISABLED, messagesService.getMessage(AUTOSCALING_DISABLED), cluster);
        notificationSender.send(cluster, history);
    }
}
