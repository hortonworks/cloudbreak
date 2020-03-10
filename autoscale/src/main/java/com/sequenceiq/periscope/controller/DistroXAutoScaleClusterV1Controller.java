package com.sequenceiq.periscope.controller;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.converter.DistroXAutoscaleClusterResponseConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class DistroXAutoScaleClusterV1Controller implements DistroXAutoScaleClusterV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXAutoScaleClusterV1Controller.class);

    @Inject
    private AutoScaleClusterCommonService asClusterCommonService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AlertController alertController;

    @Inject
    private TransactionService transactionService;

    @Inject
    private DistroXAutoscaleClusterResponseConverter distroXAutoscaleClusterResponseConverter;

    @Override
    public List<DistroXAutoscaleClusterResponse> getClusters() {
        return distroXAutoscaleClusterResponseConverter.convertAllToJson(asClusterCommonService.getDistroXClusters());
    }

    @Override
    public DistroXAutoscaleClusterResponse getClusterByCrn(String clusterCrn) {
        return createClusterJsonResponse(
                asClusterCommonService.getClusterByStackCrn(clusterCrn));
    }

    @Override
    public DistroXAutoscaleClusterResponse getClusterByName(String clusterName) {
        return createClusterJsonResponse(
                asClusterCommonService.getClusterByStackName(clusterName));
    }

    @Override
    public DistroXAutoscaleClusterResponse enableAutoscaleForClusterCrn(String clusterCrn, AutoscaleClusterState autoscaleState) {
        Cluster cluster = asClusterCommonService.getClusterByStackCrn(clusterCrn);
        return createClusterJsonResponse(asClusterCommonService.setAutoscaleState(cluster.getId(), autoscaleState));
    }

    @Override
    public DistroXAutoscaleClusterResponse enableAutoscaleForClusterName(String clusterName, AutoscaleClusterState autoscaleState) {
        Cluster cluster = asClusterCommonService.getClusterByStackName(clusterName);
        return createClusterJsonResponse(asClusterCommonService.setAutoscaleState(cluster.getId(), autoscaleState));
    }

    @Override
    public void deleteAlertsForClusterName(String clusterName) {
        asClusterCommonService.deleteAlertsForClusterName(clusterName);
    }

    @Override
    public void deleteAlertsForClusterCrn(String clusterCrn) {
        asClusterCommonService.deleteAlertsForClusterCrn(clusterCrn);
    }

    @Override
    public DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterCrn(String clusterCrn,
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        Cluster cluster = asClusterCommonService.getClusterByStackCrn(clusterCrn);
        return updateClusterAutoScaleConfig(cluster.getId(), autoscaleClusterRequest);
    }

    @Override
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

        try {
            transactionService.required(() -> {
                clusterService.deleteAlertsForCluster(clusterId);
                alertController.validateLoadAlertRequests(clusterId, autoscaleClusterRequest.getLoadAlertRequests());
                alertController.validateTimeAlertRequests(clusterId, autoscaleClusterRequest.getTimeAlertRequests());
                alertController.createLoadAlerts(clusterId, autoscaleClusterRequest.getLoadAlertRequests());
                alertController.createTimeAlerts(clusterId, autoscaleClusterRequest.getTimeAlertRequests());
                clusterService.setAutoscaleState(clusterId, autoscaleClusterRequest.getEnableAutoscaling());
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw e.getCause();
        }

        return createClusterJsonResponse(clusterService.findById(clusterId));
    }
}
