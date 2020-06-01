package com.sequenceiq.periscope.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.v1.AutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.converter.ClusterConverter;
import com.sequenceiq.periscope.domain.Cluster;

@Component
public class AutoScaleClusterV1Controller implements AutoScaleClusterV1Endpoint {

    @Inject
    private AutoScaleClusterCommonService autoScaleClusterCommonService;

    @Inject
    private ClusterConverter clusterConverter;

    @Override
    public List<AutoscaleClusterResponse> getClusters() {
        return clusterConverter.convertAllToJson(autoScaleClusterCommonService.getClusters());
    }

    @Override
    public AutoscaleClusterResponse getCluster(Long clusterId) {
        return createClusterJsonResponse(autoScaleClusterCommonService.getCluster(clusterId));
    }

    @Override
    public void deleteCluster(Long clusterId) {
        autoScaleClusterCommonService.deleteCluster(clusterId);
    }

    @Override
    public AutoscaleClusterResponse setState(Long clusterId, StateJson stateJson) {
        return createClusterJsonResponse(autoScaleClusterCommonService.setState(clusterId, stateJson));
    }

    @Override
    public AutoscaleClusterResponse setAutoscaleState(Long clusterId, AutoscaleClusterState autoscaleState) {
        return createClusterJsonResponse(autoScaleClusterCommonService.setAutoscaleState(clusterId, autoscaleState));
    }

    private AutoscaleClusterResponse createClusterJsonResponse(Cluster cluster) {
        return clusterConverter.convert(cluster);
    }

}
