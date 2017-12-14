package com.sequenceiq.periscope.controller;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.v2.AutoScaleClusterV2Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class AutoScaleClusterV2Controller implements AutoScaleClusterV2Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleClusterV2Controller.class);

    @Inject
    private AutoScaleClusterCommonService autoScaleClusterCommonService;

    @Inject
    private ClusterService clusterService;

    @Override
    public AutoscaleClusterResponse modifyByCloudbreakCluster(AutoscaleClusterRequest ambariServer, Long stackId) {
        return autoScaleClusterCommonService.modifyCluster(ambariServer, clusterService.findOneByStackId(stackId).getId());
    }

    @Override
    public AutoscaleClusterResponse getByCloudbreakCluster(Long stackId) {
        return autoScaleClusterCommonService.getCluster(clusterService.findOneByStackId(stackId).getId());
    }

    @Override
    public void deleteByCloudbreakCluster(Long stackId) {
        autoScaleClusterCommonService.deleteCluster(clusterService.findOneByStackId(stackId).getId());
    }

    @Override
    public AutoscaleClusterResponse runByCloudbreakCluster(Long stackId) {
        return autoScaleClusterCommonService.setState(clusterService.findOneByStackId(stackId).getId(), StateJson.running());
    }

    @Override
    public AutoscaleClusterResponse suspendByCloudbreakCluster(Long stackId) {
        return autoScaleClusterCommonService.setState(clusterService.findOneByStackId(stackId).getId(), StateJson.suspended());
    }

    @Override
    public AutoscaleClusterResponse enableAutoscaleStateByCloudbreakCluster(Long stackId) {
        return autoScaleClusterCommonService.setAutoscaleState(clusterService.findOneByStackId(stackId).getId(), AutoscaleClusterState.enable());
    }

    @Override
    public AutoscaleClusterResponse disableAutoscaleStateByCloudbreakCluster(Long stackId) {
        return autoScaleClusterCommonService.setAutoscaleState(clusterService.findOneByStackId(stackId).getId(), AutoscaleClusterState.disable());
    }

}
