package com.sequenceiq.periscope.controller;

import java.text.ParseException;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleClusterV1Endpoint;
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
    private DistroXAutoscaleClusterResponseConverter distroXAutoscaleClusterResponseConverter;

    @Override
    public List<DistroXAutoscaleClusterResponse> getClusters() {
        return distroXAutoscaleClusterResponseConverter.convertAllToJson(asClusterCommonService.getDistroXClusters());
    }

    @Override
    public DistroXAutoscaleClusterResponse getClusterByCrn(String clusterCrn) {
        return createClusterJsonResponse(asClusterCommonService.getClusterByStackCrn(clusterCrn));
    }

    @Override
    public DistroXAutoscaleClusterResponse getClusterByName(String clusterName) {
        return createClusterJsonResponse(
                asClusterCommonService.getClusterByStackName(clusterName));
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
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) throws ParseException {
        Cluster cluster = clusterService.findOneByStackCrn(clusterCrn);
        return updateClusterAutoScaleConfig(cluster.getId(), autoscaleClusterRequest);
    }

    @Override
    public DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterName(String clusterName,
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) throws ParseException {
        Cluster cluster = clusterService.findOneByStackName(clusterName);
        return updateClusterAutoScaleConfig(cluster.getId(), autoscaleClusterRequest);
    }

    private DistroXAutoscaleClusterResponse createClusterJsonResponse(Cluster cluster) {
        return distroXAutoscaleClusterResponseConverter.convert(cluster);
    }

    private DistroXAutoscaleClusterResponse updateClusterAutoScaleConfig(Long clusterId,
            DistroXAutoscaleClusterRequest autoscaleClusterRequest) throws ParseException {

        alertController.validateLoadAlertRequests(clusterId, autoscaleClusterRequest.getLoadAlertRequests());
        alertController.validateTimeAlertRequests(clusterId, autoscaleClusterRequest.getTimeAlertRequests());

        clusterService.deleteAlertsForCluster(clusterId);
        alertController.createLoadAlerts(clusterId, autoscaleClusterRequest.getLoadAlertRequests());
        alertController.createTimeAlerts(clusterId, autoscaleClusterRequest.getTimeAlertRequests());
        clusterService.setAutoscaleState(clusterId, autoscaleClusterRequest.getEnableAutoscaling());

        return createClusterJsonResponse(clusterService.findById(clusterId));
    }
}
