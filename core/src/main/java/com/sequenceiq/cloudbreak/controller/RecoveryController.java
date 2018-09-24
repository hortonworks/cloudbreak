package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.autoscale.RecoveryEndpoint;
import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

public class RecoveryController implements RecoveryEndpoint {

    @Inject
    private ClusterService clusterService;

    @Override
    public Response failureReport(Long stackId, FailureReport failureReport) {
        clusterService.failureReport(stackId, failureReport.getFailedNodes());
        return Response.accepted().build();
    }

    @Override
    public Response repairCluster(Long stackId, ClusterRepairRequest clusterRepairRequest) {
        clusterService.repairCluster(stackId, clusterRepairRequest.getHostGroups(), clusterRepairRequest.isRemoveOnly());
        return Response.accepted().build();
    }
}
