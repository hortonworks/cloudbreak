package com.sequenceiq.cloudbreak.api.endpoint.v1;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigsRequest;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/stacks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/cluster", description = ControllerDescription.CLUSTER_DESCRIPTION, protocols = "http,https")
public interface ClusterV1Endpoint {

    @POST
    @Path("{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.POST_FOR_STACK, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "postCluster")
    ClusterResponse post(@PathParam("id") Long id, @Valid ClusterRequest request) throws Exception;

    @GET
    @Path("{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.GET_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "getCluster")
    ClusterResponse get(@PathParam("id") Long id);

    @GET
    @Path("{id}/cluster/full")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.GET_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "getFullCluster")
    AutoscaleClusterResponse getForAutoscale(@PathParam("id") Long id);

    @GET
    @Path("account/{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "getPublicCluster")
    ClusterResponse getPublic(@PathParam("name") String name);

    @GET
    @Path("user/{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "getPrivateCluster")
    ClusterResponse getPrivate(@PathParam("name") String name);

    @DELETE
    @Path("{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.DELETE_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "deleteCluster")
    void delete(@PathParam("id") Long stackId);

    @PUT
    @Path("{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.PUT_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "putCluster")
    Response put(@PathParam("id") Long stackId, @Valid UpdateClusterJson updateJson) throws Exception;

    @POST
    @Path("{id}/cluster/config")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.GET_CLUSTER_PROPERTIES, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "getConfigsCluster")
    ConfigsResponse getConfigs(@PathParam("id") Long stackId, ConfigsRequest requests) throws Exception;

    @POST
    @Path("{id}/cluster/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.UPGRADE_AMBARI, produces = ContentType.JSON, notes = Notes.AMBARI_NOTES,
            nickname = "upgradeCluster")
    Response upgradeCluster(@PathParam("id") Long stackId, AmbariRepoDetailsJson ambariRepoDetails);

    @POST
    @Path("{id}/cluster/failurereport")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.FAILURE_REPORT, produces = ContentType.JSON, notes = Notes.FAILURE_REPORT_NOTES,
            nickname = "failureReportCluster")
    Response failureReport(@PathParam("id") Long stackId, FailureReport failureReport) throws Exception;

    @POST
    @Path("{id}/cluster/manualrepair")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.REPAIR_CLUSTER, produces = ContentType.JSON, notes = Notes.CLUSTER_REPAIR_NOTES,
            nickname = "repairCluster")
    Response repairCluster(@PathParam("id") Long stackId, ClusterRepairRequest clusterRepairRequest) throws Exception;
}
