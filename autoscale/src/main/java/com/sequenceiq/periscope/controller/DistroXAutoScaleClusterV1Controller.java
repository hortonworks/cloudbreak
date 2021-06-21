package com.sequenceiq.periscope.controller;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.controller.validation.AlertValidator;
import com.sequenceiq.periscope.converter.DistroXAutoscaleClusterResponseConverter;
import com.sequenceiq.periscope.converter.HistoryConverter;
import com.sequenceiq.periscope.converter.LoadAlertRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertRequestConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.EntitlementValidationService;
import com.sequenceiq.periscope.service.HistoryService;

@Controller
public class DistroXAutoScaleClusterV1Controller implements DistroXAutoScaleClusterV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXAutoScaleClusterV1Controller.class);

    @Inject
    private AutoScaleClusterCommonService asClusterCommonService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private HistoryService historyService;

    @Inject
    private AlertValidator alertValidator;

    @Inject
    private TransactionService transactionService;

    @Inject
    private DistroXAutoscaleClusterResponseConverter distroXAutoscaleClusterResponseConverter;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private TimeAlertRequestConverter timeAlertRequestConverter;

    @Inject
    private LoadAlertRequestConverter loadAlertRequestConverter;

    @Inject
    private EntitlementValidationService entitlementValidationService;

    @Inject
    private HistoryConverter historyConverter;

    @Override
    @DisableCheckPermissions
    public List<DistroXAutoscaleClusterResponse> getClusters() {
        return distroXAutoscaleClusterResponseConverter.convertAllToJson(asClusterCommonService.getDistroXClusters());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public DistroXAutoscaleClusterResponse getClusterByCrn(@ResourceCrn String clusterCrn) {
        return createClusterJsonResponse(
                asClusterCommonService.getClusterByStackCrn(clusterCrn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public DistroXAutoscaleClusterResponse getClusterByName(@ResourceName String clusterName) {
        return createClusterJsonResponse(
                asClusterCommonService.getClusterByStackName(clusterName));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public DistroXAutoscaleClusterResponse enableAutoscaleForClusterCrn(@ResourceCrn String clusterCrn, AutoscaleClusterState autoscaleState) {
        Cluster cluster = asClusterCommonService.getClusterByStackCrn(clusterCrn);
        return createClusterJsonResponse(asClusterCommonService.setAutoscaleState(cluster.getId(), autoscaleState));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public DistroXAutoscaleClusterResponse enableAutoscaleForClusterName(@ResourceName String clusterName, AutoscaleClusterState autoscaleState) {
        Cluster cluster = asClusterCommonService.getClusterByStackName(clusterName);
        return createClusterJsonResponse(asClusterCommonService.setAutoscaleState(cluster.getId(), autoscaleState));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public void deleteAlertsForClusterName(@ResourceName String clusterName) {
        asClusterCommonService.deleteAlertsForClusterName(clusterName);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public void deleteAlertsForClusterCrn(@ResourceCrn String clusterCrn) {
        asClusterCommonService.deleteAlertsForClusterCrn(clusterCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterCrn(@ResourceCrn String clusterCrn,
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        Cluster cluster = asClusterCommonService.getClusterByStackCrn(clusterCrn);
        return updateClusterAutoScaleConfig(cluster, autoscaleClusterRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterName(@ResourceName String clusterName,
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        Cluster cluster = asClusterCommonService.getClusterByStackName(clusterName);
        return updateClusterAutoScaleConfig(cluster, autoscaleClusterRequest);
    }

    private DistroXAutoscaleClusterResponse createClusterJsonResponse(Cluster cluster) {
        return distroXAutoscaleClusterResponseConverter.convert(cluster);
    }

    private DistroXAutoscaleClusterResponse updateClusterAutoScaleConfig(Cluster cluster,
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) {

        alertValidator.validateEntitlementAndDisableIfNotEntitled(cluster);
        alertValidator.validateDistroXAutoscaleClusterRequest(cluster, autoscaleClusterRequest);

        try {
            transactionService.required(() -> {
                clusterService.deleteAlertsForCluster(cluster.getId());
                asClusterCommonService.createLoadAlerts(cluster.getId(),
                        loadAlertRequestConverter.convertAllFromJson(autoscaleClusterRequest.getLoadAlertRequests()));
                asClusterCommonService.createTimeAlerts(cluster.getId(),
                        timeAlertRequestConverter.convertAllFromJson(autoscaleClusterRequest.getTimeAlertRequests()));
                asClusterCommonService.setAutoscaleState(cluster.getId(), autoscaleClusterRequest.getEnableAutoscaling());
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw e.getCause();
        }

        Cluster updatedCluster = clusterService.findById(cluster.getId());
        asClusterCommonService.processAutoscalingStateChanged(updatedCluster);
        return createClusterJsonResponse(updatedCluster);
    }
}
