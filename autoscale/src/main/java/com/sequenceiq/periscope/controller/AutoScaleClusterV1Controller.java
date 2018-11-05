package com.sequenceiq.periscope.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.v1.AutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.StateJson;

@Component
public class AutoScaleClusterV1Controller implements AutoScaleClusterV1Endpoint {

    @Inject
    private AutoScaleClusterCommonService autoScaleClusterCommonService;

    @Override
    public List<AutoscaleClusterResponse> getClusters() {
        return autoScaleClusterCommonService.getClusters();
    }

    @Override
    public AutoscaleClusterResponse getCluster(Long clusterId) {
        return autoScaleClusterCommonService.getCluster(clusterId);
    }

    @Override
    public void deleteCluster(Long clusterId) {
        autoScaleClusterCommonService.deleteCluster(clusterId);
    }

    @Override
    public AutoscaleClusterResponse setState(Long clusterId, StateJson stateJson) {
        return autoScaleClusterCommonService.setState(clusterId, stateJson);
    }

    @Override
    public AutoscaleClusterResponse setAutoscaleState(Long clusterId, AutoscaleClusterState autoscaleState) {
        return autoScaleClusterCommonService.setAutoscaleState(clusterId, autoscaleState);
    }
}
