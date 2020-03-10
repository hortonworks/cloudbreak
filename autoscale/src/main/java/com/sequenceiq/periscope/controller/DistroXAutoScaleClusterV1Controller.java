package com.sequenceiq.periscope.controller;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.model.AutoscalingModeType;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.converter.DistroXClusterRequestConverter;
import com.sequenceiq.periscope.converter.DistroXClusterResponseConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class DistroXAutoScaleClusterV1Controller implements DistroXAutoScaleClusterV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXAutoScaleClusterV1Controller.class);

    @Inject
    private AutoScaleClusterCommonService asClusterCommonService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AlertService alertService;

    @Inject
    private DistroXClusterResponseConverter distroXClusterResponseConverter;

    @Inject
    private DistroXClusterRequestConverter distroXClusterRequestConverter;

    @Override
    public List<DistroXAutoscaleClusterResponse> getClusters() {
        return distroXClusterResponseConverter.convertAllToJson(asClusterCommonService.getDistroXClusters());
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
    public DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterCrn(String clusterCrn, DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        Cluster cluster = clusterService.findOneByStackCrn(clusterCrn);
        return updateClusterAutoScaleConfig(cluster.getId(), cluster.getStackCrn(), autoscaleClusterRequest);
    }

    @Override
    public DistroXAutoscaleClusterResponse updateAutoscaleConfigByClusterName(String clusterName, DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        Cluster cluster = clusterService.findOneByStackName(clusterName);
        return updateClusterAutoScaleConfig(cluster.getId(), cluster.getStackCrn(), autoscaleClusterRequest);
    }

    private DistroXAutoscaleClusterResponse createClusterJsonResponse(Cluster cluster) {
        return distroXClusterResponseConverter.convert(cluster);
    }

    private DistroXAutoscaleClusterResponse updateClusterAutoScaleConfig(Long clusterId,
            String stackCrn, DistroXAutoscaleClusterRequest autoscaleClusterRequest) {
        Cluster distroXCluster = distroXClusterRequestConverter.convert(autoscaleClusterRequest);
        if (distroXCluster.isAutoscalingEnabled() != null) {
            clusterService.setAutoscaleState(clusterId, distroXCluster.isAutoscalingEnabled());
        }

        if (autoscaleClusterRequest.getScalingConfiguration() != null) {
            clusterService.updateScalingConfiguration(clusterId, autoscaleClusterRequest.getScalingConfiguration());
        }

        AutoscalingModeType autoScalingMode = distroXCluster.getAutoscalingMode();
        if (autoScalingMode != null) {
            switch (autoScalingMode) {
                case LOAD_BASED:
                    alertService.createOrUpdateLoadAlerts(clusterId, distroXCluster.getLoadAlerts());
                    break;
                case TIME_BASED:
                    alertService.createOrUpdateTimeAlerts(clusterId, distroXCluster.getTimeAlerts());
                    break;
                default:
                    throw new BadRequestException(
                            String.format("AutoScalingMode %s is not supported for clusterCRN %s", autoScalingMode, stackCrn));
            }
        }
        return createClusterJsonResponse(clusterService.findById(clusterId));
    }
}
