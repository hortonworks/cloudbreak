package com.sequenceiq.cloudbreak.api.endpoint.autoscale;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/recovery")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/recovery", description = ControllerDescription.RECOVERY_DESCRIPTION, protocols = "http,https")
public interface RecoveryEndpoint {

    @POST
    @Path("/stack/{id}/cluster/failurereport")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.FAILURE_REPORT, produces = ContentType.JSON, notes = Notes.FAILURE_REPORT_NOTES,
            nickname = "failureReportClusterForAutoscale")
    Response failureReport(@PathParam("id") Long stackId, FailureReport failureReport);

    @POST
    @Path("/stack/{id}/cluster/manualrepair")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.REPAIR_CLUSTER, produces = ContentType.JSON, notes = Notes.CLUSTER_REPAIR_NOTES,
            nickname = "repairCluster")
    Response repairCluster(@PathParam("id") Long stackId, ClusterRepairRequest clusterRepairRequest);

}
