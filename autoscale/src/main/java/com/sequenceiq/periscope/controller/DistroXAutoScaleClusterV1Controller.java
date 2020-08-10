package com.sequenceiq.periscope.controller;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.converter.DistroXAutoscaleClusterResponseConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;

@Controller
@AuthorizationResource
public class DistroXAutoScaleClusterV1Controller implements DistroXAutoScaleClusterV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXAutoScaleClusterV1Controller.class);

    @Inject
    private AutoScaleClusterCommonService asClusterCommonService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private HistoryService historyService;

    @Inject
    private AlertController alertController;

    @Inject
    private TransactionService transactionService;

    @Inject
    private DistroXAutoscaleClusterResponseConverter distroXAutoscaleClusterResponseConverter;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_READ)
    public List<DistroXAutoscaleClusterResponse> getClusters() {
        return distroXAutoscaleClusterResponseConverter.convertAllToJson(asClusterCommonService.getDistroXClusters());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_READ)
    public DistroXAutoscaleClusterResponse getClusterByCrn(String clusterCrn) {
        return createClusterJsonResponse(
                asClusterCommonService.getClusterByStackCrn(clusterCrn));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_READ)
    public DistroXAutoscaleClusterResponse getClusterByName(String clusterName) {
        return createClusterJsonResponse(
                asClusterCommonService.getClusterByStackName(clusterName));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public DistroXAutoscaleClusterResponse enableAutoscaleForClusterCrn(String clusterCrn, AutoscaleClusterState autoscaleState) {
        Cluster cluster = asClusterCommonService.getClusterByStackCrn(clusterCrn);
        return createClusterJsonResponse(asClusterCommonService.setAutoscaleState(cluster.getId(), autoscaleState));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public DistroXAutoscaleClusterResponse enableAutoscaleForClusterName(String clusterName, AutoscaleClusterState autoscaleState) {
        Cluster cluster = asClusterCommonService.getClusterByStackName(clusterName);
        return createClusterJsonResponse(asClusterCommonService.setAutoscaleState(cluster.getId(), autoscaleState));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public void deleteAlertsForClusterName(String clusterName) {
        asClusterCommonService.deleteAlertsForClusterName(clusterName);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public void deleteAlertsForClusterCrn(String clusterCrn) {
        asClusterCommonService.deleteAlertsForClusterCrn(clusterCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterCrn(String clusterCrn,
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        Cluster cluster = asClusterCommonService.getClusterByStackCrn(clusterCrn);
        return updateClusterAutoScaleConfig(cluster.getId(), autoscaleClusterRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_WRITE)
    public DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterName(String clusterName,
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        Cluster cluster = asClusterCommonService.getClusterByStackName(clusterName);
        return updateClusterAutoScaleConfig(cluster.getId(), autoscaleClusterRequest);
    }

    private DistroXAutoscaleClusterResponse createClusterJsonResponse(Cluster cluster) {
        return distroXAutoscaleClusterResponseConverter.convert(cluster);
    }

    private DistroXAutoscaleClusterResponse updateClusterAutoScaleConfig(Long clusterId,
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) {

        alertController.validateLoadAlertRequests(clusterId, autoscaleClusterRequest.getLoadAlertRequests());
        alertController.validateTimeAlertRequests(clusterId, autoscaleClusterRequest.getTimeAlertRequests());

        try {
            transactionService.required(() -> {
                clusterService.deleteAlertsForCluster(clusterId);
                alertController.createLoadAlerts(clusterId, autoscaleClusterRequest.getLoadAlertRequests());
                alertController.createTimeAlerts(clusterId, autoscaleClusterRequest.getTimeAlertRequests());
                asClusterCommonService.setAutoscaleState(clusterId, autoscaleClusterRequest.getEnableAutoscaling());
            });
            createHistoryAndNotifyConfigChange(clusterId);
        } catch (TransactionService.TransactionExecutionException e) {
            throw e.getCause();
        }

        return createClusterJsonResponse(clusterService.findById(clusterId));
    }

    private void createHistoryAndNotifyConfigChange(long clusterId) {
        Cluster cluster = clusterService.findById(clusterId);
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

        History history = historyService.createEntry(scalingStatus, statusMessage, cluster);
        notificationSender.send(cluster, history);
    }
}
