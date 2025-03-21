package com.sequenceiq.periscope.controller;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.periscope.api.endpoint.v2.AutoScaleClusterV2Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.StateJson;
import com.sequenceiq.periscope.converter.ClusterConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.ClusterService;

/**
 * @deprecated Database ID based endpoints are deprecated for removal.
 */
@Controller
@Deprecated(since = "CB 2.26.0", forRemoval = true)
@InternalOnly
public class AutoScaleClusterV2Controller implements AutoScaleClusterV2Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoScaleClusterV2Controller.class);

    @Inject
    private AutoScaleClusterCommonService autoScaleClusterCommonService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterConverter clusterConverter;

    @AccountIdNotNeeded
    @Override
    public AutoscaleClusterResponse getByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        return createClusterJsonResponse(
                autoScaleClusterCommonService.getCluster(cluster.getId()));
    }

    @AccountIdNotNeeded
    @Override
    public void deleteByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        autoScaleClusterCommonService.deleteCluster(cluster.getId());
    }

    @AccountIdNotNeeded
    @Override
    public AutoscaleClusterResponse runByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        return createClusterJsonResponse(
                autoScaleClusterCommonService.setState(cluster.getId(), StateJson.running()));
    }

    @AccountIdNotNeeded
    @Override
    public AutoscaleClusterResponse suspendByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        return createClusterJsonResponse(
                autoScaleClusterCommonService.setState(cluster.getId(), StateJson.suspended()));
    }

    @AccountIdNotNeeded
    @Override
    public AutoscaleClusterResponse enableAutoscaleStateByCloudbreakCluster(Long stackId) {
        Cluster cluster = clusterService.findOneByStackId(stackId);
        return createClusterJsonResponse(
                autoScaleClusterCommonService.setAutoscaleState(cluster.getId(), AutoscaleClusterState.enable()));
    }

    @AccountIdNotNeeded
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
