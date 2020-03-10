package com.sequenceiq.periscope.controller;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.v2.AutoScaleClusterV2Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.converter.ClusterConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class AutoScaleClusterV2Controller implements AutoScaleClusterV2Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleClusterV2Controller.class);

    @Inject
    private AutoScaleClusterCommonService autoScaleClusterCommonService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterConverter clusterConverter;

    @Override
    public AutoscaleClusterResponse getByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        return createClusterJsonResponse(
                autoScaleClusterCommonService.getCluster(cluster.getId()));
    }

    @Override
    public void deleteByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        autoScaleClusterCommonService.deleteCluster(cluster.getId());
    }

    @Override
    public AutoscaleClusterResponse runByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        return createClusterJsonResponse(
                autoScaleClusterCommonService.setState(cluster.getId(), StateJson.running()));
    }

    @Override
    public AutoscaleClusterResponse suspendByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        return createClusterJsonResponse(
                autoScaleClusterCommonService.setState(cluster.getId(), StateJson.suspended()));
    }

    @Override
    public AutoscaleClusterResponse enableAutoscaleStateByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        return createClusterJsonResponse(
                autoScaleClusterCommonService.setAutoscaleState(cluster.getId(), AutoscaleClusterState.enable()));
    }

    @Override
    public AutoscaleClusterResponse disableAutoscaleStateByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        return createClusterJsonResponse(
                autoScaleClusterCommonService.setAutoscaleState(cluster.getId(), AutoscaleClusterState.disable()));
    }

    private AutoscaleClusterResponse createClusterJsonResponse(Cluster cluster) {
        return clusterConverter.convert(cluster);
    }

}
