package com.sequenceiq.periscope.controller;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterServerCertUpdateRequest;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.controller.validation.AlertValidator;
import com.sequenceiq.periscope.converter.DistroXAutoscaleClusterResponseConverter;
import com.sequenceiq.periscope.converter.LoadAlertRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertRequestConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.security.SecurityConfigService;

@Controller
public class DistroXAutoScaleClusterV1Controller implements DistroXAutoScaleClusterV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXAutoScaleClusterV1Controller.class);

    @Inject
    private AutoScaleClusterCommonService asClusterCommonService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AlertValidator alertValidator;

    @Inject
    private TransactionService transactionService;

    @Inject
    private DistroXAutoscaleClusterResponseConverter distroXAutoscaleClusterResponseConverter;

    @Inject
    private TimeAlertRequestConverter timeAlertRequestConverter;

    @Inject
    private LoadAlertRequestConverter loadAlertRequestConverter;

    @Inject
    private SecurityConfigService securityConfigService;

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
        return updateClusterAutoScaleState(cluster, autoscaleState);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public DistroXAutoscaleClusterResponse enableAutoscaleForClusterName(@ResourceName String clusterName, AutoscaleClusterState autoscaleState) {
        Cluster cluster = asClusterCommonService.getClusterByStackName(clusterName);
        return updateClusterAutoScaleState(cluster, autoscaleState);
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

    @InternalOnly
    @Override
    public void updateServerCertificate(@RequestObject DistroXAutoscaleClusterServerCertUpdateRequest request) {
        Optional<Cluster> optionalCluster = clusterService.findOneByStackCrn(request.getCrn());
        optionalCluster.ifPresent(cluster -> securityConfigService.updateServerCertInSecurityConfig(cluster, request.getNewServerCert()));
    }

    private DistroXAutoscaleClusterResponse createClusterJsonResponse(Cluster cluster) {
        return distroXAutoscaleClusterResponseConverter.convert(cluster);
    }

    private DistroXAutoscaleClusterResponse updateClusterAutoScaleState(Cluster cluster, AutoscaleClusterState autoscaleState) {
        setPendingState(!autoscaleState.isEnableAutoscaling(), cluster.getId());
        if (autoscaleState.isEnableAutoscaling()) {
            alertValidator.validateIfStackIsAvailable(cluster);
        }
        if (Boolean.TRUE.equals(autoscaleState.getUseStopStartMechanism())) {
            alertValidator.validateStopStartEntitlementAndDisableIfNotEntitled(cluster);
        }
        alertValidator.validateScheduleWithStopStart(cluster, autoscaleState);
        try {
            transactionService.required(() -> asClusterCommonService.setAutoscaleState(cluster.getId(), autoscaleState));
        } catch (TransactionService.TransactionExecutionException e) {
            throw e.getCause();
        }
        Cluster updatedCluster = clusterService.findById(cluster.getId());
        asClusterCommonService.processAutoscalingStateChanged(updatedCluster);
        return createClusterJsonResponse(updatedCluster);
    }

    private DistroXAutoscaleClusterResponse updateClusterAutoScaleConfig(Cluster cluster,
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        setPendingState(!autoscaleClusterRequest.getEnableAutoscaling(), cluster.getId());

        if (autoscaleClusterRequest.getEnableAutoscaling()) {
            alertValidator.validateIfStackIsAvailable(cluster);
        }
        alertValidator.validateEntitlementAndDisableIfNotEntitled(cluster);
        alertValidator.validateDistroXAutoscaleClusterRequest(cluster, autoscaleClusterRequest);
        alertValidator.validateScheduleWithStopStart(autoscaleClusterRequest);

        if (Boolean.TRUE.equals(autoscaleClusterRequest.getUseStopStartMechanism())) {
            alertValidator.validateStopStartEntitlementAndDisableIfNotEntitled(cluster);
        }

        String policyHostGroup = asClusterCommonService.determineLoadBasedPolicyHostGroup(cluster).orElse(null);
        try {
            transactionService.required(() -> {
                clusterService.deleteAlertsForCluster(cluster.getId());
                asClusterCommonService.createLoadAlerts(cluster.getId(),
                        loadAlertRequestConverter.convertAllFromJson(autoscaleClusterRequest.getLoadAlertRequests()));
                asClusterCommonService.createTimeAlerts(cluster.getId(),
                        timeAlertRequestConverter.convertAllFromJson(autoscaleClusterRequest.getTimeAlertRequests()));
                asClusterCommonService.setAutoscaleState(cluster.getId(), autoscaleClusterRequest.getEnableAutoscaling());
                asClusterCommonService.setStopStartScalingState(cluster.getId(), autoscaleClusterRequest.getUseStopStartMechanism(),
                        !ObjectUtils.isEmpty(autoscaleClusterRequest.getTimeAlertRequests()),
                        !ObjectUtils.isEmpty(autoscaleClusterRequest.getLoadAlertRequests()));
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw e.getCause();
        }

        Cluster updatedCluster = clusterService.findById(cluster.getId());
        if (updatedCluster.getLoadAlerts().isEmpty()) {
            asClusterCommonService.deleteStoppedNodesIfPresent(updatedCluster.getId(), policyHostGroup);
        }
        asClusterCommonService.processAutoscalingConfigChanged(updatedCluster);
        return createClusterJsonResponse(updatedCluster);
    }

    private void setPendingState(Boolean disableAutoscaling, Long clusterId) {
        if (disableAutoscaling) {
            StateJson stateJson = new StateJson();
            stateJson.setState(ClusterState.PENDING);
            asClusterCommonService.setState(clusterId, stateJson);
        }
    }
}
