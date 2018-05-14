package com.sequenceiq.periscope.controller;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.periscope.api.endpoint.v2.AutoScaleClusterV2Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.StateJson;
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
    private AuthorizationService authorizationService;

    @Override
    public AutoscaleClusterResponse modifyByCloudbreakCluster(AutoscaleClusterRequest ambariServer, Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        authorizationService.hasWritePermission(cluster);
        return autoScaleClusterCommonService.modifyCluster(ambariServer, cluster.getId());
    }

    @Override
    public AutoscaleClusterResponse getByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        authorizationService.hasReadPermission(cluster);
        return autoScaleClusterCommonService.getCluster(cluster.getId());
    }

    @Override
    public void deleteByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        authorizationService.hasWritePermission(cluster);
        autoScaleClusterCommonService.deleteCluster(cluster.getId());
    }

    @Override
    public AutoscaleClusterResponse runByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        authorizationService.hasWritePermission(cluster);
        return autoScaleClusterCommonService.setState(cluster.getId(), StateJson.running());
    }

    @Override
    public AutoscaleClusterResponse suspendByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        authorizationService.hasWritePermission(cluster);
        return autoScaleClusterCommonService.setState(cluster.getId(), StateJson.suspended());
    }

    @Override
    public AutoscaleClusterResponse enableAutoscaleStateByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        authorizationService.hasWritePermission(cluster);
        return autoScaleClusterCommonService.setAutoscaleState(cluster.getId(), AutoscaleClusterState.enable());
    }

    @Override
    public AutoscaleClusterResponse disableAutoscaleStateByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        authorizationService.hasWritePermission(cluster);
        return autoScaleClusterCommonService.setAutoscaleState(cluster.getId(), AutoscaleClusterState.disable());
    }

}
