package com.sequenceiq.periscope.controller;


import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;

@Component
public class AutoScaleClusterCommonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleClusterCommonService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    public List<Cluster> getClusters() {
        return clusterService.findAllByUser(restRequestThreadLocalService.getCloudbreakUser());
    }

    public List<Cluster> getDistroXClusters() {
        return clusterService.findDistroXByUser(restRequestThreadLocalService.getCloudbreakUser());
    }

    public Cluster getCluster(Long clusterId) {
        return clusterService.findById(clusterId);
    }

    public Cluster getClusterByStackCrn(String stackCrn) {
        return clusterService.findOneByStackCrn(stackCrn);
    }

    public Cluster getClusterByStackName(String stackName) {
        return clusterService.findOneByStackName(stackName);
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
        Cluster cluster = clusterService.setAutoscaleState(clusterId, enableAutoScaling);
        createHistoryAndNotification(cluster);
        return cluster;
    }

    private void createHistoryAndNotification(Cluster cluster) {
        History history;
        history = cluster.isAutoscalingEnabled()
                ? historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster)
                : historyService.createEntry(ScalingStatus.DISABLED, "Autoscaling has been disabled for the cluster.", 0, cluster);
        notificationSender.send(cluster, history);
    }

    public void deleteAlertsForClusterCrn(String stackCrn) {
        Cluster cluster = clusterService.findOneByStackCrn(stackCrn);
        clusterService.deleteAlertsForCluster(cluster.getId());
    }

    public void deleteAlertsForClusterName(String stackName) {
        Cluster cluster = clusterService.findOneByStackName(stackName);
        clusterService.deleteAlertsForCluster(cluster.getId());
    }
}
